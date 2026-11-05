"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const friendCodeUtil_1 = require("./friendCodeUtil");
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
describe("Friend Code Shared Normalization Vectors (TypeScript)", () => {
    const vectorsPath = path.resolve(__dirname, "../../docs/friend-code-vectors.json");
    const vectors = JSON.parse(fs.readFileSync(vectorsPath, "utf8"));
    test("runs all 40+ vectors from shared friend-code-vectors.json", () => {
        for (const vec of vectors) {
            const res = (0, friendCodeUtil_1.normalizeFriendCode)(vec.input);
            if (vec.valid) {
                expect(res.valid).toBe(true);
                expect(res.canonicalCode).toBe(vec.expected);
            }
            else {
                expect(res.valid).toBe(false);
            }
        }
    });
});
//# sourceMappingURL=friendCodeUtil.test.js.map