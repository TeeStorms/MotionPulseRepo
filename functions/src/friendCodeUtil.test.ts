import { normalizeFriendCode } from "./friendCodeUtil";
import * as fs from "fs";
import * as path from "path";

describe("Friend Code Shared Normalization Vectors (TypeScript)", () => {
  const vectorsPath = path.resolve(__dirname, "../../docs/friend-code-vectors.json");
  const vectors = JSON.parse(fs.readFileSync(vectorsPath, "utf8"));

  test("runs all 40+ vectors from shared friend-code-vectors.json", () => {
    for (const vec of vectors) {
      const res = normalizeFriendCode(vec.input);
      if (vec.valid) {
        expect(res.valid).toBe(true);
        expect(res.canonicalCode).toBe(vec.expected);
      } else {
        expect(res.valid).toBe(false);
      }
    }
  });
});
