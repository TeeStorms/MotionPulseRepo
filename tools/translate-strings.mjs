import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const KEY_PATH = 'C:\\keys\\translate-key.json.json';
const LANGUAGES = ['af', 'tn', 'zu', 'ts'];
const STRINGS_PATH = path.resolve('app/src/main/res/values/strings.xml');
const SCOPE_PATH = path.resolve('docs/translations/scope.txt');
const REPORT_PATH = path.resolve('docs/translations/last-run.md');
const OVERRIDES_PATH = path.resolve('docs/translations/overrides.json');

function ensureDir(filePath) {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
}

function base64url(str) {
    return Buffer.from(str)
        .toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_');
}

async function getAccessToken(serviceAccount) {
    const now = Math.floor(Date.now() / 1000);
    const header = { alg: 'RS256', typ: 'JWT' };
    const claimSet = {
        iss: serviceAccount.client_email,
        scope: 'https://www.googleapis.com/auth/cloud-translation',
        aud: 'https://oauth2.googleapis.com/token',
        exp: now + 3600,
        iat: now
    };

    const encodedHeader = base64url(JSON.stringify(header));
    const encodedClaimSet = base64url(JSON.stringify(claimSet));
    const signatureInput = `${encodedHeader}.${encodedClaimSet}`;

    const signer = crypto.createSign('RSA-SHA256');
    signer.update(signatureInput);
    const signature = signer.sign(serviceAccount.private_key, 'base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_');

    const jwt = `${signatureInput}.${signature}`;

    const res = await fetch('https://oauth2.googleapis.com/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
            grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            assertion: jwt
        })
    });

    const data = await res.json();
    if (!res.ok) {
        throw new Error(`Failed to get OAuth token: ${JSON.stringify(data)}`);
    }
    return data.access_token;
}

function protectPlaceholders(text) {
    const placeholders = [];
    let protectedText = text;

    // Protect format specifiers %1$s, %2$d, %s, %d, \n
    const regex = /(%[0-9]+\$[a-zA-Z]|%[a-zA-Z]|\\n)/g;
    protectedText = protectedText.replace(regex, (match) => {
        const token = `__FMT_${placeholders.length}__`;
        placeholders.push({ token, original: match });
        return token;
    });

    return { protectedText, placeholders };
}

function restorePlaceholders(text, placeholders) {
    let restored = text;
    for (const item of placeholders) {
        // Regex replace token with original format specifier
        restored = restored.replace(new RegExp(item.token, 'g'), item.original);
    }
    // Escape single quotes for Android XML
    restored = restored.replace(/\\'/g, "'").replace(/'/g, "\\'");
    return restored;
}

async function translateText(accessToken, projectId, text, targetLanguage) {
    const { protectedText, placeholders } = protectPlaceholders(text);

    const url = `https://translation.googleapis.com/v3/projects/${projectId}:translateText`;
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            contents: [protectedText],
            targetLanguageCode: targetLanguage,
            sourceLanguageCode: 'en',
            mimeType: 'text/plain'
        })
    });

    const data = await res.json();
    if (!res.ok) {
        throw new Error(`Translation API error for ${targetLanguage}: ${JSON.stringify(data)}`);
    }

    const translatedRaw = data.translations[0].translatedText;
    return restorePlaceholders(translatedRaw, placeholders);
}

async function main() {
    const isDryRun = !process.argv.includes('--run');
    const isAll = process.argv.includes('--all');
    console.log(`[Translate Script] Mode: ${isDryRun ? 'DRY-RUN' : 'LIVE RUN'}`);

    if (!fs.existsSync(STRINGS_PATH)) {
        console.error(`Error: strings.xml not found at ${STRINGS_PATH}`);
        process.exit(1);
    }

    let scopedKeys = null;
    if (!isAll && fs.existsSync(SCOPE_PATH)) {
        const scopeContent = fs.readFileSync(SCOPE_PATH, 'utf8');
        scopedKeys = new Set(
            scopeContent.split('\n')
                .map(line => line.trim())
                .filter(line => line && !line.startsWith('#'))
        );
        console.log(`Loaded ${scopedKeys.size} scoped keys from ${SCOPE_PATH}`);
    }

    const xmlContent = fs.readFileSync(STRINGS_PATH, 'utf8');
    const stringMatches = [...xmlContent.matchAll(/(<string name="([^"]+)"(?:\s+translatable="false")?[^>]*>(.*?)<\/string>)/gs)];
    const pluralMatches = [...xmlContent.matchAll(/(<plurals name="([^"]+)">([\s\S]*?)<\/plurals>)/gs)];

    let totalItems = 0;
    let totalChars = 0;

    for (const match of stringMatches) {
        const fullTag = match[1];
        const key = match[2];
        const val = match[3];
        if (fullTag.includes('translatable="false"')) continue;
        if (scopedKeys && !scopedKeys.has(key)) continue;

        totalItems++;
        totalChars += val.length;
    }

    for (const match of pluralMatches) {
        const key = match[2];
        const inner = match[3];
        if (scopedKeys && !scopedKeys.has(key)) continue;

        totalItems++;
        totalChars += inner.length;
    }

    console.log(`\n--- Translation Summary ---`);
    console.log(`In-scope items to translate: ${totalItems}`);
    console.log(`Character count: ${totalChars}`);
    console.log(`Estimated cost ($0.20 per 1M chars): $${(((totalChars * LANGUAGES.length) / 1000000) * 0.20).toFixed(6)}`);
    console.log(`Languages targeted: ${LANGUAGES.join(', ')}`);

    let overrides = {};
    if (fs.existsSync(OVERRIDES_PATH)) {
        try {
            overrides = JSON.parse(fs.readFileSync(OVERRIDES_PATH, 'utf8'));
            console.log(`Loaded human overrides for: ${Object.keys(overrides).join(', ')}`);
        } catch (e) {
            console.warn('Warning: Could not parse overrides.json');
        }
    }

    let reportLines = [`# Translation Last Run Report\n\n- Mode: ${isDryRun ? 'DRY-RUN' : 'LIVE RUN'}\n- In-scope items count: ${totalItems}\n- Total chars: ${totalChars}\n`];

    if (!isDryRun) {
        if (!fs.existsSync(KEY_PATH)) {
            console.error(`Service account key not found at ${KEY_PATH}`);
            process.exit(1);
        }

        const serviceAccount = JSON.parse(fs.readFileSync(KEY_PATH, 'utf8'));
        const accessToken = await getAccessToken(serviceAccount);
        console.log('Successfully authenticated with Google Cloud Translation API.');

        for (const lang of LANGUAGES) {
            console.log(`Translating keys to ${lang}...`);
            const langDir = path.resolve(`app/src/main/res/values-${lang}`);
            ensureDir(path.join(langDir, 'strings.xml'));

            const langOverrides = overrides[lang] || {};
            let outXml = `<?xml version="1.0" encoding="utf-8"?>\n<!-- Machine translated draft / human overrides, needs native review -->\n<resources>\n`;

            let translatedCount = 0;
            let overrideCount = 0;

            for (const match of stringMatches) {
                const fullTag = match[1];
                const key = match[2];
                const defaultVal = match[3];
                if (fullTag.includes('translatable="false"')) continue;
                if (scopedKeys && !scopedKeys.has(key)) continue;

                let val = langOverrides[key];
                if (val) {
                    val = val.replace(/\\'/g, "'").replace(/'/g, "\\'");
                    overrideCount++;
                } else {
                    val = await translateText(accessToken, serviceAccount.project_id, defaultVal, lang);
                    translatedCount++;
                }

                outXml += `    <string name="${key}">${val}</string>\n`;
            }

            for (const match of pluralMatches) {
                const key = match[2];
                const inner = match[3];
                if (scopedKeys && !scopedKeys.has(key)) continue;

                let val = langOverrides[key];
                if (val) {
                    overrideCount++;
                    outXml += `    <plurals name="${key}">\n${val}\n    </plurals>\n`;
                } else {
                    translatedCount++;
                    const itemMatches = [...inner.matchAll(/<item quantity="([^"]+)">(.*?)<\/item>/g)];
                    outXml += `    <plurals name="${key}">\n`;
                    for (const itemMatch of itemMatches) {
                        const qty = itemMatch[1];
                        const itemVal = itemMatch[2];
                        const transItem = await translateText(accessToken, serviceAccount.project_id, itemVal, lang);
                        outXml += `        <item quantity="${qty}">${transItem}</item>\n`;
                    }
                    outXml += `    </plurals>\n`;
                }
            }

            outXml += `</resources>\n`;
            fs.writeFileSync(path.join(langDir, 'strings.xml'), outXml, 'utf8');
            console.log(`Wrote localized resources for values-${lang}/strings.xml (API: ${translatedCount}, Overrides: ${overrideCount})`);
            reportLines.push(`### Language \`${lang}\`:\n- Translated via API: ${translatedCount}\n- Human Overrides: ${overrideCount}\n- Status: OK (No 'needs human translation' flags)\n`);
        }
    }

    ensureDir(REPORT_PATH);
    fs.writeFileSync(REPORT_PATH, reportLines.join('\n'), 'utf8');
    console.log(`Report written to ${REPORT_PATH}`);

    if (isDryRun) {
        console.log(`\n[Dry Run] Completed successfully. Pass --run to execute live translation via Google Cloud Translation API.`);
    }
}

main().catch(err => {
    console.error('Translation script error:', err);
    process.exit(1);
});
