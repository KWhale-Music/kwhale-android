package org.dueattendant149.kwhale.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.MainActivity
import org.dueattendant149.kwhale.R

/**
 * Home-screen player widget inspired by VK X's atmospheric cover-tinted layout:
 * squircle artwork on the left, dynamic radial gradient extracted from the current
 * cover, crisp white track title, pastel cover-tinted artist subtitle, queue shortcut,
 * and generous transport controls.
 */
class PlayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 84.dp),
            DpSize(250.dp, 100.dp),
            DpSize(320.dp, 116.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KwhaleApp
        val snapshot = PlayerSnapshotStore(context).load()
        val liveState = app.player.state.value
        val live = liveState.current
        val useLive = live != null && (snapshot == null || snapshot.trackId != live.id)

        val title = if (useLive) live!!.title else snapshot?.title ?: "KWhale"
        val artist = if (useLive) live!!.artist else snapshot?.artist ?: "Нет активного трека"
        val coverUrl = if (useLive) live!!.coverUrl else snapshot?.coverUrl
        val isPlaying = if (useLive) liveState.isPlaying else snapshot?.isPlaying ?: liveState.isPlaying

        val assets = loadWidgetVisuals(context, coverUrl)
        val rootAction = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )

        provideContent {
            val widgetSize = LocalSize.current
            val compact = widgetSize.width < 230.dp || widgetSize.height < 96.dp
            val wide = widgetSize.width >= 300.dp
            val coverSize = when {
                compact -> 66.dp
                wide -> 94.dp
                else -> 86.dp
            }
            val outerPad = if (compact) 9.dp else 11.dp
            val maxTitleChars = when {
                wide -> 38
                compact -> 24
                else -> 30
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(Color(0xFF141218))
                    .clickable(rootAction),
            ) {
                Image(
                    provider = ImageProvider(assets.backdrop),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxSize(),
                )

                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = outerPad, vertical = outerPad),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (assets.cover != null) {
                        Image(
                            provider = ImageProvider(assets.cover),
                            contentDescription = "Обложка",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.size(coverSize),
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .size(coverSize)
                                .cornerRadius(if (compact) 14.dp else 18.dp)
                                .background(Color(0xFF231E33)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_music_note),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(ColorProvider(assets.artistColor)),
                                modifier = GlanceModifier.size(if (compact) 28.dp else 36.dp),
                            )
                        }
                    }

                    Spacer(GlanceModifier.width(if (compact) 10.dp else 14.dp))

                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .padding(top = if (compact) 0.dp else 2.dp),
                            ) {
                                Text(
                                    text = ellipsize(title, maxTitleChars),
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFF8F9FB)),
                                        fontSize = if (compact) 15.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(GlanceModifier.height(if (compact) 1.dp else 2.dp))
                                Text(
                                    text = ellipsize(artist, maxTitleChars),
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(assets.artistColor),
                                        fontSize = if (compact) 12.sp else 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }

                            Box(
                                modifier = GlanceModifier
                                    .size(if (compact) 28.dp else 32.dp)
                                    .cornerRadius(16.dp)
                                    .clickable(actionRunCallback<OpenQueueAction>()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_queue_music),
                                    contentDescription = "Очередь",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color(0xF0FFFFFF))),
                                    modifier = GlanceModifier.size(if (compact) 19.dp else 22.dp),
                                )
                            }
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = if (compact) 0.dp else 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ControlButton(
                                icon = R.drawable.ic_previous,
                                hitSize = if (compact) 36 else 44,
                                iconSize = if (compact) 22 else 26,
                                description = "Предыдущий трек",
                                action = actionRunCallback<PreviousTrackAction>(),
                            )
                            Spacer(GlanceModifier.width(if (compact) 8.dp else 20.dp))
                            ControlButton(
                                icon = if (isPlaying) R.drawable.ic_pause else R.drawable.player_play,
                                hitSize = if (compact) 40 else 48,
                                iconSize = if (compact) 28 else 34,
                                description = if (isPlaying) "Пауза" else "Играть",
                                action = actionRunCallback<TogglePlaybackAction>(),
                            )
                            Spacer(GlanceModifier.width(if (compact) 8.dp else 20.dp))
                            ControlButton(
                                icon = R.drawable.ic_next,
                                hitSize = if (compact) 36 else 44,
                                iconSize = if (compact) 22 else 26,
                                description = "Следующий трек",
                                action = actionRunCallback<NextTrackAction>(),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadWidgetVisuals(context: Context, url: String?): WidgetVisuals {
        val rawCover = if (url.isNullOrBlank()) {
            null
        } else {
            withTimeoutOrNull(3_000) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val result = Coil.imageLoader(context).execute(
                            ImageRequest.Builder(context)
                                .data(url)
                                .allowHardware(false)
                                .size(COVER_PX)
                                .build(),
                        )
                        val drawable = (result as? SuccessResult)?.drawable ?: return@runCatching null
                        drawable.toBitmap(COVER_PX, COVER_PX, Bitmap.Config.ARGB_8888)
                    }.getOrNull()
                }
            }
        }
        return withContext(Dispatchers.Default) {
            val palette = extractPalette(rawCover)
            val roundedCover = rawCover?.let { roundBitmap(it, it.width * 0.19f) }
            val backdrop = buildBackdropBitmap(palette.primaryGlow, palette.secondaryGlow)
            WidgetVisuals(
                cover = roundedCover,
                backdrop = backdrop,
                artistColor = Color(palette.artistTint),
            )
        }
    }

    private fun extractPalette(bitmap: Bitmap?): WidgetPalette {
        if (bitmap == null) {
            return WidgetPalette(
                primaryGlow = 0xFF422F70.toInt(),
                secondaryGlow = 0xFF282B5E.toInt(),
                artistTint = 0xFFC5BBE0.toInt(),
            )
        }
        val sample = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val hsl = FloatArray(3)
        var bestHue = 265f
        var bestSat = 0f
        var bestScore = -1f
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        val count = sample.width * sample.height

        for (y in 0 until sample.height) {
            for (x in 0 until sample.width) {
                val px = sample.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                sumR += r
                sumG += g
                sumB += b

                ColorUtils.RGBToHSL(r, g, b, hsl)
                val s = hsl[1]
                val l = hsl[2]
                // Prefer rich mid-tone / vibrant pigments while suppressing near-white/black noise.
                if (l in 0.12f..0.85f && s > 0.16f) {
                    val lightnessWeight = 1f - kotlin.math.abs(l - 0.45f)
                    val score = s * 2.2f + lightnessWeight * 0.5f
                    if (score > bestScore) {
                        bestScore = score
                        bestHue = hsl[0]
                        bestSat = s
                    }
                }
            }
        }
        if (sample !== bitmap) sample.recycle()

        if (bestScore < 0f) {
            val avg = ((sumR / count).toInt() shl 16) or
                ((sumG / count).toInt() shl 8) or
                (sumB / count).toInt() or
                (0xFF shl 24)
            ColorUtils.colorToHSL(avg, hsl)
            bestHue = hsl[0]
            bestSat = hsl[1].coerceAtLeast(0.18f)
        }

        val primaryGlow = ColorUtils.HSLToColor(
            floatArrayOf(bestHue, bestSat.coerceIn(0.40f, 0.88f), 0.25f),
        )
        val secondaryHue = (bestHue + 18f) % 360f
        val secondaryGlow = ColorUtils.HSLToColor(
            floatArrayOf(secondaryHue, bestSat.coerceIn(0.30f, 0.75f), 0.21f),
        )
        val artistTint = ColorUtils.HSLToColor(
            floatArrayOf(bestHue, bestSat.coerceIn(0.24f, 0.52f), 0.74f),
        )
        return WidgetPalette(primaryGlow, secondaryGlow, artistTint)
    }

    private fun buildBackdropBitmap(primaryGlow: Int, secondaryGlow: Int): Bitmap {
        val w = BACKDROP_W
        val h = BACKDROP_H
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Base deep obsidian diagonal foundation
        val baseTop = ColorUtils.blendARGB(0xFF131118.toInt(), primaryGlow, 0.28f)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(baseTop, 0xFF121017.toInt(), 0xFF16131D.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Left ambient cover bleed behind the artwork squircle
        val leftGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.16f,
                h * 0.45f,
                w * 0.46f,
                intArrayOf(
                    ColorUtils.setAlphaComponent(primaryGlow, 215),
                    ColorUtils.setAlphaComponent(primaryGlow, 85),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), leftGlowPaint)

        // Upper-right secondary glow echoing the artwork mood across the widget card
        val rightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.90f,
                h * 0.12f,
                w * 0.42f,
                intArrayOf(
                    ColorUtils.setAlphaComponent(secondaryGlow, 170),
                    ColorUtils.setAlphaComponent(secondaryGlow, 55),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), rightGlowPaint)
        return out
    }

    private fun roundBitmap(source: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
            radius,
            radius,
            paint,
        )
        return output
    }

    private fun ellipsize(value: String, max: Int): String =
        if (value.length <= max) value else value.take((max - 1).coerceAtLeast(1)).trimEnd() + "…"

    private data class WidgetPalette(
        val primaryGlow: Int,
        val secondaryGlow: Int,
        val artistTint: Int,
    )

    private data class WidgetVisuals(
        val cover: Bitmap?,
        val backdrop: Bitmap,
        val artistColor: Color,
    )

    private companion object {
        const val COVER_PX = 200
        const val BACKDROP_W = 420
        const val BACKDROP_H = 160
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    hitSize: Int,
    iconSize: Int,
    description: String,
    action: Action,
) {
    Box(
        modifier = GlanceModifier
            .size(hitSize.dp)
            .cornerRadius((hitSize / 2).dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFF6F7FA))),
            modifier = GlanceModifier.size(iconSize.dp),
        )
    }
}

class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerWidget()
}

class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        val app = context.applicationContext as KwhaleApp
        app.player.toggle()
        PlayerWidget().updateAll(context)
    }
}

class NextTrackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        val app = context.applicationContext as KwhaleApp
        app.player.next()
        PlayerWidget().updateAll(context)
    }
}

class PreviousTrackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        val app = context.applicationContext as KwhaleApp
        app.player.previous()
        PlayerWidget().updateAll(context)
    }
}

class OpenQueueAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_QUEUE, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }
}
