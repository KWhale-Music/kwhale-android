package org.dueattendant149.kwhale.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.design.KwhaleAccent
import org.dueattendant149.kwhale.design.KwhaleAccents
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.KwhaleTheme
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.design.ThemeMode
import org.dueattendant149.kwhale.design.mix

/**
 * Appearance screen.
 *
 * Two things changed from the old layout:
 *  - 129 accents used to be dumped as a 6×9 wall of unlabelled circles. They are
 *    now grouped, laid out in a horizontally scrollable strip per family, and
 *    preceded by a live preview so the choice is visible before it is made.
 *  - the selected mode chip used to be tinted with a fixed brown. It now follows
 *    the active theme's container colours, which is what the user is choosing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    currentMode: ThemeMode,
    currentAccentId: Int,
    monet: Boolean,
    onBack: () -> Unit,
    onChanged: (ThemeMode, Int, Boolean) -> Unit,
) {
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }
    var selectedAccent by remember(currentAccentId) { mutableIntStateOf(currentAccentId) }
    var dynamicColor by remember(monet) { mutableStateOf(monet) }

    val modes = listOf(
        ThemeMode.SYSTEM to "Системная",
        ThemeMode.DARK to "Тёмная",
        ThemeMode.LIGHT to "Светлая",
        ThemeMode.AMOLED to "AMOLED",
    )

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
            contentPadding = PaddingValues(
                start = KwhaleSpacing.gutter,
                end = KwhaleSpacing.gutter,
                bottom = KwhaleSpacing.xxl + LocalMiniPlayerBottomPadding.current,
            ),
        ) {
            item {
                Text(
                    text = "Режим",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = KwhaleSpacing.xs, bottom = KwhaleSpacing.xs),
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(KwhaleSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(KwhaleSpacing.xs),
                ) {
                    modes.forEach { (mode, label) ->
                        ModeChip(
                            label = label,
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                onChanged(mode, selectedAccent, dynamicColor)
                            },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KwhaleSpacing.md)
                        .clip(RoundedCornerShape(14.dp))
                        .toggleable(
                            value = dynamicColor,
                            role = Role.Switch,
                            onValueChange = {
                                dynamicColor = it
                                onChanged(selectedMode, selectedAccent, it)
                            },
                        )
                        .padding(vertical = KwhaleSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Динамический цвет", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Цвета из системных обоев (Android 12+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(KwhaleSpacing.sm))
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = null,
                    )
                }
            }

            // ── live preview ─────────────────────────────────────────────────
            item {
                Text(
                    text = "Предпросмотр",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = KwhaleSpacing.lg, bottom = KwhaleSpacing.xs),
                )
            }
            item {
                AppearancePreview(
                    mode = selectedMode,
                    accent = KwhaleAccents.byId(selectedAccent),
                    monet = dynamicColor,
                )
            }

            if (dynamicColor) {
                item {
                    Text(
                        text = "Включён динамический цвет — выбор акцента отключит его автоматически",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = KwhaleSpacing.sm),
                    )
                }
            }

            // ── accents, grouped ─────────────────────────────────────────────
            AccentFamilies.forEach { family ->
                item {
                    Text(
                        text = family.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = KwhaleSpacing.lg, bottom = KwhaleSpacing.xs),
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier
                            .selectableGroup()
                            .alpha(if (dynamicColor) 0.72f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(KwhaleSpacing.sm),
                        contentPadding = PaddingValues(vertical = KwhaleSpacing.xxs),
                    ) {
                        items(family.accents, key = { it.id }) { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = !dynamicColor && accent.id == selectedAccent,
                                onClick = {
                                    selectedAccent = accent.id
                                    dynamicColor = false
                                    onChanged(selectedMode, selectedAccent, false)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mode chip with spring-animated selection.
 *
 * A plain FilterChip could not express "this is the active surface colour"
 * without borrowing the primary colour, which clashed with the accent list.
 */
@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(),
        label = "modeContainer",
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = KwhaleSpacing.md, vertical = KwhaleSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

/** Round accent swatch with animated ring. */
@Composable
private fun AccentSwatch(accent: KwhaleAccent, selected: Boolean, onClick: () -> Unit) {
    val ringWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = spring(),
        label = "swatchRing",
    )
    val size by animateDpAsState(
        targetValue = if (selected) 52.dp else 46.dp,
        animationSpec = spring(),
        label = "swatchSize",
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .semantics { contentDescription = accent.name }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (ringWidth > 0.dp) {
                        Modifier.border(
                            BorderStroke(ringWidth, MaterialTheme.colorScheme.onSurface),
                            CircleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(accent.color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (accent.color.luminance() > 0.55f) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Miniature of the real player: rendered under the user's selected theme
 * (brightness + accent or dynamic wallpaper palette) so the preview stays truthful.
 */
@Composable
private fun AppearancePreview(
    mode: ThemeMode,
    accent: KwhaleAccent,
    monet: Boolean,
) {
    KwhaleTheme(mode = mode, accent = accent, monet = monet) {
        val scheme = MaterialTheme.colorScheme
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainer,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(KwhaleSpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(scheme.primary, scheme.primary.mix(Color.Black, 0.45f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(KwhaleSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Ночной эфир",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                        )
                        Text(
                            "Мира Ким",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(KwhaleSpacing.xs))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary),
                            )
                        }
                    }
                    Spacer(Modifier.width(KwhaleSpacing.sm))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(KwhaleSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(KwhaleSpacing.xs)) {
                    listOf("Слушать", "В очередь").forEachIndexed { index, label ->
                        Surface(
                            shape = CircleShape,
                            color = if (index == 0) scheme.primary else scheme.secondaryContainer,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (index == 0) scheme.onPrimary else scheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── accent families ────────────────────────────────────────────────────────
// Hand-grouped so 129 swatches read as a palette instead of noise. Order
// inside a family follows the catalogue, so ids stay stable.

private data class AccentFamily(val title: String, val accents: List<KwhaleAccent>)

private val AccentFamilies: List<AccentFamily> = run {
    val all = KwhaleAccents.all
    fun pick(vararg ids: Int): List<KwhaleAccent> =
        ids.toList().mapNotNull { id -> all.firstOrNull { it.id == id } }

    listOf(
        AccentFamily(
            "Базовые",
            pick(0, 1, 2, 3, 11, 12, 13, 14, 15, 16),
        ),
        AccentFamily(
            "Тёплые",
            all.filter { it.id in 20..45 && it.color.isWarm() },
        ),
        AccentFamily(
            "Холодные",
            all.filter { it.id in 20..45 && !it.color.isWarm() },
        ),
        AccentFamily(
            "Природа",
            all.filter { it.id in 46..75 },
        ),
        AccentFamily(
            "Сообщество",
            all.filter { it.id in 76..105 },
        ),
        AccentFamily(
            "Системные",
            all.filter { it.id >= 106 },
        ),
    ).filter { it.accents.isNotEmpty() }
}

private fun Color.isWarm(): Boolean {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            255,
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt(),
        ),
        hsv,
    )
    val hue = hsv[0]
    return hue < 90f || hue > 300f
}