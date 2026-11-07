package org.dueattendant149.kwhale.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.dueattendant149.kwhale.R
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.ThemeMode

class PlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KwhaleApp
        // App-consistent M3 palette (same derivation as KwhaleTheme) instead of the
        // legacy KwhaleThemeCatalog, so the widget matches the in-app theme.
        val dark = app.settings.themeMode != ThemeMode.LIGHT
        val amoled = app.settings.themeMode == ThemeMode.AMOLED
        val bg = if (!dark) 0xFFFFFFFF.toInt() else if (amoled) 0xFF000000.toInt() else 0xFF121212.toInt()
        val textPrimary = if (!dark) 0xFF1B1C1E.toInt() else 0xFFE6E1E5.toInt()
        val textSecondary = if (!dark) 0xFF49454F.toInt() else 0xFFCAC4D0.toInt()
        val state = app.player.state.value
        val track = state.current
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(bg))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = "Обложка",
                        modifier = GlanceModifier.size(52.dp),
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                    ) {
                        Text(
                            text = track?.title ?: "KWhale",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(textPrimary)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = track?.artist ?: "Нет трека",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(textSecondary)),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                    Button(
                        text = if (state.isPlaying) "❚❚" else "▶",
                        onClick = actionRunCallback<TogglePlaybackAction>(),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Button(
                        text = "›",
                        onClick = actionRunCallback<NextTrackAction>(),
                    )
                }
            }
        }
    }
}

class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerWidget()
}

class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
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
        parameters: ActionParameters,
    ) {
        val app = context.applicationContext as KwhaleApp
        app.player.next()
        PlayerWidget().updateAll(context)
    }
}
