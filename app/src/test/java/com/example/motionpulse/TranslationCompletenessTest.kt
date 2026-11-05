package com.example.motionpulse

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TranslationCompletenessTest {

    @Test
    fun verifyAllLocalesHaveCompleteKeysAndPlurals() {
        val resDir = File("src/main/res")
        val defaultFile = File(resDir, "values/strings.xml")
        assertTrue("Default strings.xml must exist", defaultFile.exists())
        val defaultStrings = extractStrings(defaultFile)
        val defaultPlurals = extractPlurals(defaultFile)

        val locales = listOf("values-af", "values-tn", "values-zu", "values-ts")
        for (locale in locales) {
            val localeFile = File(resDir, "$locale/strings.xml")
            assertTrue("Locale file $locale/strings.xml must exist", localeFile.exists())
            val localeStrings = extractStrings(localeFile)
            val localePlurals = extractPlurals(localeFile)

            for ((key, _) in defaultStrings) {
                assertTrue("Key '$key' is missing in $locale", localeStrings.containsKey(key))
            }

            for ((key, quantities) in defaultPlurals) {
                assertTrue("Plural '$key' is missing in $locale", localePlurals.containsKey(key))
                val locQuantities = localePlurals[key] ?: emptySet()
                assertTrue("Plural '$key' in $locale must contain 'other'", locQuantities.contains("other"))
            }
        }
    }

    private fun extractStrings(file: File): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val doc = parseXml(file)
        val nodes = doc.getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue
            val translatable = node.attributes?.getNamedItem("translatable")?.nodeValue
            if (name != null && translatable != "false") {
                map[name] = node.textContent ?: ""
            }
        }
        return map
    }

    private fun extractPlurals(file: File): Map<String, Set<String>> {
        val map = mutableMapOf<String, Set<String>>()
        val doc = parseXml(file)
        val nodes = doc.getElementsByTagName("plurals")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue
            if (name != null) {
                val quantities = mutableSetOf<String>()
                val items = node.childNodes
                for (j in 0 until items.length) {
                    val itemNode = items.item(j)
                    if (itemNode.nodeName == "item") {
                        val q = itemNode.attributes?.getNamedItem("quantity")?.nodeValue
                        if (q != null) quantities.add(q)
                    }
                }
                map[name] = quantities
            }
        }
        return map
    }

    private fun parseXml(file: File): Document {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        return dBuilder.parse(file)
    }
}
