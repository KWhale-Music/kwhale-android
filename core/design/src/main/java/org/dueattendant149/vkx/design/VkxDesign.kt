package org.dueattendant149.vkx.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Design tokens shared by the whole client.
 *
 * Before this file the app used raw literals (20.dp, 28.dp, 12.dp…) scattered
 * across screens, which made the rhythm drift between them. Everything visual
 * should now come from here.
 */

/** 4/8 dp rhythm. Screen gutter is the only value a screen should hardcode. */
object VkxSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp

    /** Horizontal page gutter, identical on every screen. */
    val gutter = 20.dp

    /** Cover thumbnail inside a list row. */
    val cover = 48.dp

    /** Extra bottom space a scroll list needs for the floating player. */
    val miniPlayer = 72.dp
    val navBar = 80.dp
}

/** Spring-based motion (M3 Expressive). Replaces one-off tween durations. */
object VkxMotion {

    /** Route-to-route transitions: begin and end on screen. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Element entering the screen. */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Element leaving the screen (faster than enter). */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val durationFast = 180
    const val durationStd = 280
    const val durationEmphasized = 420

    /** Bouncy press / like feedback. */
    fun <T> springy() = SpringSpec<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * Deterministic cover palette.
 *
 * 520 of 564 tracks carry a real cover; the rest (and every test/offline row)
 * used to fall back to an empty grey plate that read as a broken image. Instead
 * we derive two harmonious hues from the track/album name so a cover-less row
 * still looks deliberate — and stays stable across restarts and devices.
 */
object VkxCoverPalette {

    fun seedOf(vararg parts: String?): String =
        parts.filterNotNull().firstOrNull { it.isNotBlank() }?.trim().orEmpty()

    private fun hash(seed: String): Int {
        // FNV-1a: cheap, stable across JVM versions (String.hashCode is too, but
        // this spreads short Cyrillic titles better).
        var h = -0x7ee3623b
        for (ch in seed) {
            h = h xor ch.code
            h *= 0x01000193
        }
        return abs(if (h == Int.MIN_VALUE) 0 else h)
    }

    /**
     * Two-stop gradient for a cover placeholder.
     *
     * [dark] keeps placeholders deep so white initials stay legible; light
     * themes get pastel stops instead of muddy dark blocks.
     */
    fun gradient(seed: String, dark: Boolean): List<Color> {
        val h = hash(seed.ifBlank { "vkx" })
        val hue = (h % 360).toFloat()
        val hue2 = (hue + 38f + (h / 360 % 24)) % 360f

        return if (dark) {
            listOf(
                Color.hsl(hue, 0.42f, 0.34f),
                Color.hsl(hue2, 0.48f, 0.16f),
            )
        } else {
            listOf(
                Color.hsl(hue, 0.62f, 0.82f),
                Color.hsl(hue2, 0.55f, 0.66f),
            )
        }
    }

    /** Readable foreground for the initial drawn on top of [gradient]. */
    fun onGradient(seed: String, dark: Boolean): Color {
        val stop = gradient(seed, dark).first()
        return if (stop.luminance() > 0.5f) Color(0xFF14161A) else Color.White
    }

    fun initialOf(seed: String): String {
        val clean = seed.trim().trimStart('«', '"', '\'', '(', '[')
        val ch = clean.firstOrNull() ?: return ""
        return ch.uppercaseChar().toString()
    }
}

/** Small colour helpers used by placeholders and dynamic surfaces. */
fun Color.mix(other: Color, fraction: Float): Color = lerp(this, other, fraction.coerceIn(0f, 1f))

fun Color.isLightColor(): Boolean = luminance() > 0.5f

/**
 * Russian pluralisation for counters ("1 трек / 3 трека / 5 треков").
 *
 * The UI printed `${n} треков` everywhere, so a one-track playlist read
 * "1 треков". One helper instead of ten copies of the same bug.
 */
object VkxPlural {

    fun tracks(n: Int): String = "${formatted(n)} ${form(n, "трек", "трека", "треков")}"

    fun playlists(n: Int): String = "${formatted(n)} ${form(n, "плейлист", "плейлиста", "плейлистов")}"

    fun albums(n: Int): String = "${formatted(n)} ${form(n, "альбом", "альбома", "альбомов")}"

    fun artists(n: Int): String = "${formatted(n)} ${form(n, "артист", "артиста", "артистов")}"

    fun listeners(n: Int): String = "${formatted(n)} ${form(n, "слушатель", "слушателя", "слушателей")}"

    /** 1.2 тыс. / 3.4 млн — keeps long counters inside a 40 dp column. */
    fun formatted(n: Int): String = when {
        n >= 1_000_000 -> "%.1f млн".format(n / 1_000_000f)
        n >= 10_000 -> "%.0f тыс.".format(n / 1_000f)
        n >= 1_000 -> "%.1f тыс.".format(n / 1_000f)
        else -> n.toString()
    }

    private fun form(n: Int, one: String, few: String, many: String): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..14 -> many
            mod10 == 1 -> one
            mod10 in 2..4 -> few
            else -> many
        }
    }
}