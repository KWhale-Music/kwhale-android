package org.dueattendant149.kwhale.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
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
 * Home-screen player card, matching the VK X "player small" widget:
 * dark rounded card, large rounded cover on the left, title/artist,
 * queue button top-right, transport controls centred at the bottom.
 *
 * The widget reads [PlayerSnapshotStore], not the in-memory MediaController
 * state: the launcher can instantiate the provider in a process where the
 * controller has not connected yet, and that used to leave only the loading
 * placeholder. Tapping the card opens MainActivity with EXTRA_OPEN_PLAYER.
 */
class PlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KwhaleApp
        val snapshot = PlayerSnapshotStore(context).load()
        val liveState = app.player.state.value
        val live = liveState.current
        val useLive = live != null && (snapshot == null || snapshot.trackId != live.id)

        val title = if (useLive) live!!.title else snapshot?.title ?: "KWhale"
        val artist = if (useLive) live!!.artist else snapshot?.artist ?: "Нет трека"
        val coverUrl = if (useLive) live!!.coverUrl else snapshot?.coverUrl
        val isPlaying = if (useLive) liveState.isPlaying else snapshot?.isPlaying ?: liveState.isPlaying

        val cover = loadCover(context, coverUrl)
        val coverProvider = cover?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ic_music_note)
        val rootAction = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(Color(0xFF17141F))
                    .padding(10.dp)
                    .clickable(rootAction),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = coverProvider,
                        contentDescription = "Обложка",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.size(80.dp),
                    )
                    Spacer(GlanceModifier.width(10.dp))
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
                            Column(GlanceModifier.defaultWeight()) {
                                Text(
                                    text = ellipsize(title, 26),
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFF5F2FA)),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(GlanceModifier.height(2.dp))
                                Text(
                                    text = ellipsize(artist, 24),
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFC7A9B6)),
                                        fontSize = 13.sp,
                                    ),
                                )
                            }
                            Image(
                                provider = ImageProvider(R.drawable.ic_queue_music),
                                contentDescription = "Очередь",
                                modifier = GlanceModifier
                                    .size(24.dp)
                                    .clickable(actionRunCallback<OpenQueueAction>()),
                            )
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Control(
                                icon = R.drawable.ic_previous,
                                size = 30,
                                description = "Предыдущий трек",
                                action = actionRunCallback<PreviousTrackAction>(),
                            )
                            Spacer(GlanceModifier.width(12.dp))
                            Control(
                                icon = if (isPlaying) R.drawable.ic_pause else R.drawable.player_play,
                                size = 34,
                                description = if (isPlaying) "Пауза" else "Играть",
                                action = actionRunCallback<TogglePlaybackAction>(),
                            )
                            Spacer(GlanceModifier.width(12.dp))
                            Control(
                                icon = R.drawable.ic_next,
                                size = 30,
                                description = "Следующий трек",
                                action = actionRunCallback<NextTrackAction>(),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadCover(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withTimeoutOrNull(3_000) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val result = Coil.imageLoader(context).execute(
                        ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .size(256)
                            .build(),
                    )
                    val drawable = (result as? SuccessResult)?.drawable ?: return@runCatching null
                    val raw = drawable.toBitmap(256, 256, Bitmap.Config.ARGB_8888)
                    roundBitmap(raw, raw.width * 0.18f)
                }.getOrNull()
            }
        }
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
        if (value.length <= max) value else value.take(max - 1).trimEnd() + "…"
}

@Composable
private fun Control(icon: Int, size: Int, description: String, action: Action) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = description,
        modifier = GlanceModifier
            .size(size.dp)
            .clickable(action),
    )
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
