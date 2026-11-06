package org.dueattendant149.vkx.widget

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
import org.dueattendant149.vkx.R
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.design.VkxThemeCatalog

class PlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as VkxApp
        val theme = VkxThemeCatalog.byId(app.settings.themeId)
        provideContent {
            val state = app.player.state.value
            val track = state.current
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(theme.bgPage.toInt()))
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
                            text = track?.title ?: "VK X",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(theme.textPrimary.toInt())),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = track?.artist ?: "Нет трека",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(theme.textSecondary.toInt())),
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
        val app = context.applicationContext as VkxApp
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
        val app = context.applicationContext as VkxApp
        app.player.next()
        PlayerWidget().updateAll(context)
    }
}
