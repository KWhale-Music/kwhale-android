package org.dueattendant149.vkx.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.dueattendant149.vkx.design.ThemeMode
import org.dueattendant149.vkx.design.VkxAccents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    currentMode: ThemeMode,
    currentAccentId: Int,
    monet: Boolean,
    onBack: () -> Unit,
    onChanged: (ThemeMode, Int, Boolean) -> Unit,
) {
    val modes = listOf(
        ThemeMode.LIGHT to "Светлая",
        ThemeMode.DARK to "Тёмная",
        ThemeMode.AMOLED to "AMOLED",
        ThemeMode.MONET to "Системная",
    )
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }
    var selectedAccent by remember(currentAccentId) { mutableStateOf(currentAccentId) }
    var dynamicColor by remember(monet) { mutableStateOf(monet) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Оформление") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 28.dp,
            ),
        ) {
            item {
                Text(
                    text = "Режим",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    modes.forEach { (mode, label) ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                dynamicColor = mode == ThemeMode.MONET
                                onChanged(mode, selectedAccent, dynamicColor)
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Динамический цвет", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Цвета из системных обоев",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = {
                            dynamicColor = it
                            onChanged(
                                if (it) ThemeMode.MONET else selectedMode,
                                selectedAccent,
                                it,
                            )
                        },
                    )
                }
            }
            item {
                Text(
                    text = "Акцент",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
            }
            items(VkxAccents.all.chunked(6).size) { rowIndex ->
                val row = VkxAccents.all.chunked(6)[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { accent ->
                        val selected = accent.id == selectedAccent
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accent.color, CircleShape)
                                .then(
                                    if (selected) Modifier.border(
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                                        CircleShape,
                                    ) else Modifier,
                                )
                                .clickable {
                                    selectedAccent = accent.id
                                    onChanged(selectedMode, selectedAccent, dynamicColor)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (accent.color.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Предпросмотр",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ночной эфир", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Мира Ким · Тихий сигнал",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Акцент применяется к действиям",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

