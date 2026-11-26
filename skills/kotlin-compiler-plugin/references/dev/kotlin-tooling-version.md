# `KotlinToolingVersion` with Maturity Comparison

## 動機

`CompatContext.Factory` 選択で「`minVersion ≤ current` を満たす中で最大」 を計算するとき、 単純な `compareTo` 比較では Beta/RC の順序付けが破綻する。

- `KotlinToolingVersion("2.4.0") > KotlinToolingVersion("2.4.0-Beta2")` であって欲しい (STABLE > BETA)
- `KotlinToolingVersion("2.4.0-RC2") > KotlinToolingVersion("2.4.0-RC1")` であって欲しい (高い RC 番号)
- `KotlinToolingVersion("2.4.0-Beta2") > KotlinToolingVersion("2.4.0-Beta1")` も同様
- dev / SNAPSHOT は最も低位

これを実現する `KotlinToolingVersion` class が必要。

## 原典

Metro の `compiler-compat/.../KotlinToolingVersion.kt` (Apache 2.0) からの adapt 推奨。 リポジトリの compat module 選択ロジック向けに trim する。

## 実装 (Trim 版)

```kotlin
package me.tbsten.compose.preview.lab.compiler.compat

/**
 * Kotlin tooling version with maturity-aware comparison.
 *
 * Examples:
 * - "2.3.0", "2.3.21", "2.4.0-Beta2"
 * - "2.4.0-dev-2124"
 * - "2.3.0-RC", "2.3.0-RC2"
 */
public class KotlinToolingVersion(
    public val major: Int,
    public val minor: Int,
    public val patch: Int,
    public val classifier: String?,
) : Comparable<KotlinToolingVersion> {

    public val maturity: Maturity = run {
        val c = classifier?.lowercase()
        when {
            c == null || c.matches(Regex("""(release-)?\d+""")) -> Maturity.STABLE
            c == "snapshot" -> Maturity.SNAPSHOT
            c.matches(Regex("""rc\d*(-release)?(-?\d+)?""")) -> Maturity.RC
            c.matches(Regex("""beta\d*(-release)?(-?\d+)?""")) -> Maturity.BETA
            c.matches(Regex("""alpha\d*(-release)?(-?\d+)?""")) -> Maturity.ALPHA
            c.matches(Regex("""m\d+(-release)?(-\d+)?""")) -> Maturity.MILESTONE
            else -> Maturity.DEV
        }
    }

    public val isDev: Boolean get() = maturity == Maturity.DEV

    private val classifierNumber: Int? by lazy {
        classifier?.let { Regex("""(?:rc|beta|alpha|m)(\d+)""", RegexOption.IGNORE_CASE).find(it) }
            ?.groupValues?.get(1)?.toIntOrNull()
    }

    private val buildNumber: Int? by lazy {
        // Trailing number such as in "dev-2124".
        classifier?.let { Regex("""-(\d+)$""").find(it) }?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Compares two versions in the order: major → minor → patch → [Maturity] → classifier
     * number → build number → classifier string (tiebreaker).
     *
     * [Maturity] ordinal order (ascending): SNAPSHOT < DEV < MILESTONE < ALPHA < BETA < RC < STABLE
     */
    override fun compareTo(other: KotlinToolingVersion): Int {
        (this.major - other.major).takeIf { it != 0 }?.let { return it }
        (this.minor - other.minor).takeIf { it != 0 }?.let { return it }
        (this.patch - other.patch).takeIf { it != 0 }?.let { return it }
        (this.maturity.ordinal - other.maturity.ordinal).takeIf { it != 0 }?.let { return it }
        // For STABLE: a missing classifier ranks higher than "release-N".
        if (this.classifier == null && other.classifier != null) return 1
        if (this.classifier != null && other.classifier == null) return -1
        val a = classifierNumber ?: 0
        val b = other.classifierNumber ?: 0
        (a - b).takeIf { it != 0 }?.let { return it }
        val ab = buildNumber ?: 0
        val bb = other.buildNumber ?: 0
        (ab - bb).takeIf { it != 0 }?.let { return it }
        // Tiebreaker: classifier 文字列比較で compareTo == 0 が equals と一致するようにする
        val ac = this.classifier?.lowercase()
        val bc = other.classifier?.lowercase()
        if (ac != bc) return ac?.compareTo(bc ?: return 1) ?: -1
        return 0
    }

    override fun equals(other: Any?): Boolean = other is KotlinToolingVersion && compareTo(other) == 0

    override fun hashCode(): Int {
        var r = major
        r = 31 * r + minor
        r = 31 * r + patch
        r = 31 * r + (classifier?.lowercase()?.hashCode() ?: 0)
        return r
    }

    override fun toString(): String =
        if (classifier != null) "$major.$minor.$patch-$classifier" else "$major.$minor.$patch"

    public enum class Maturity { SNAPSHOT, DEV, MILESTONE, ALPHA, BETA, RC, STABLE }
}

public fun KotlinToolingVersion(versionString: String): KotlinToolingVersion {
    val base = versionString.substringBefore('-')
    val classifier = if ('-' in versionString) versionString.substringAfter('-') else null
    val parts = base.split('.')
    return KotlinToolingVersion(
        major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
        minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        patch = parts.getOrNull(2)?.toIntOrNull() ?: 0,
        classifier = classifier,
    )
}
```

## 比較例 (テストケース)

```kotlin
KotlinToolingVersion("2.3.0")          > KotlinToolingVersion("2.3.0-Beta2")     // STABLE > BETA
KotlinToolingVersion("2.4.0-RC2")      > KotlinToolingVersion("2.4.0-RC1")       // 高い RC 番号
KotlinToolingVersion("2.4.0-Beta2")    > KotlinToolingVersion("2.4.0-Beta1")     // 同上
KotlinToolingVersion("2.4.0-dev-2124") > KotlinToolingVersion("2.4.0-dev-100")   // 高い build 番号
KotlinToolingVersion("2.3.0")          > KotlinToolingVersion("2.3.0-release-1") // null classifier > non-null
KotlinToolingVersion("2.3.21")         > KotlinToolingVersion("2.3.20")          // patch
KotlinToolingVersion("2.4.0")          > KotlinToolingVersion("2.3.21")          // minor
```

## Factory 選択への影響例

```
登録 Factory:
  compat-k230       minVersion="2.3.0"
  compat-k2320      minVersion="2.3.20"
  compat-k2321      minVersion="2.3.21"
  compat-k240_beta2 minVersion="2.4.0-Beta2"

runtime = "2.4.0-Beta2":
  全 Factory が compatible ✅
  max(2.3.0, 2.3.20, 2.3.21, 2.4.0-Beta2) = 2.4.0-Beta2 → compat-k240_beta2 が選択

runtime = "2.4.0" (stable):
  全 Factory が compatible (2.4.0-Beta2 < 2.4.0 なので) ✅
  max = 2.4.0-Beta2 (まだ stable 用 Factory が登録されてなければ)
  → 新 stable 用に compat-k240 を作るか、 compat-k240_beta2 で十分か判断
```

## Property-Based Test 推奨

Kotest property test で広範に挙動を確認しておく:

```kotlin
class KotlinToolingVersionTest : FunSpec({
    test("STABLE always greater than any classifier on same major.minor.patch") {
        checkAll<Int, Int, Int>(iterations = 5000) { major, minor, patch ->
            val ma = (major and 0x7fff)
            val mi = (minor and 0x7fff)
            val pa = (patch and 0x7fff)
            val stable = KotlinToolingVersion("$ma.$mi.$pa")
            listOf("Beta1", "Beta2", "RC1", "RC2", "Alpha1", "dev-123").forEach { c ->
                stable shouldBeGreaterThan KotlinToolingVersion("$ma.$mi.$pa-$c")
            }
        }
    }

    test("compareTo is consistent with equals") {
        checkAll<String>(iterations = 5000) { /* random valid version strings */ ... }
    }
})
```
