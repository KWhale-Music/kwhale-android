package org.dueattendant149.kwhale.design

/** Generated from assets/theme_catalog.json (37 themes). */
data class KwhaleCatalogTheme(
    val id: Int,
    val name: String,
    val group: String,
    val light: Boolean,
    val accentId: Int,
    val bgPage: Long,
    val bgPrimary: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val dockBackground: Long,
)

object KwhaleThemeCatalog {
    val all: List<KwhaleCatalogTheme> = listOf(
        KwhaleCatalogTheme(0, "Темная", "default", false, 2, 0xFF121212, 0xFF202020, 0xFFFFFFFF, 0xFF8E8E93, 0xFF121212),
        KwhaleCatalogTheme(1, "Светлая", "default", true, 1, 0xFFFFFFFF, 0xFFF0F0F0, 0xFF111111, 0xFF6B6B70, 0xFFFFFFFF),
        KwhaleCatalogTheme(2, "AMOLED", "default", false, 2, 0xFF000000, 0xFF121212, 0xFFFFFFFF, 0xFF8E8E93, 0xFF000000),
        KwhaleCatalogTheme(3, "Темно-синяя", "dark", false, 3, 0xFF151D26, 0xFF1C2733, 0xFFFFFFFF, 0xFF8E8E93, 0xFF1C2733),
        KwhaleCatalogTheme(4, "Temporary Bliss", "community", false, 4, 0xFF000131, 0xFF000127, 0xFFF6C894, 0xFFF6C894, 0xFF000127),
        KwhaleCatalogTheme(5, "OwO Dark", "community", false, 5, 0xFF18191B, 0xFF1C1D1F, 0xFFFFFFFF, 0xFF8E8E93, 0xFF1C1D1F),
        KwhaleCatalogTheme(6, "Rena", "community", false, 6, 0xFF212735, 0xFF212735, 0xFFFFFFFF, 0xFF8E8E93, 0xFF212735),
        KwhaleCatalogTheme(7, "AEX", "dark", false, 7, 0xFF242038, 0xFF332D4F, 0xFFFFFFFF, 0xFF8E8E93, 0xFF332D4F),
        KwhaleCatalogTheme(8, "Modified Dark", "community", false, 8, 0xFF131722, 0xFF19212E, 0xFFFFFFFF, 0xFF8E8E93, 0xFF19212E),
        KwhaleCatalogTheme(9, "Неоновая", "community", false, 9, 0xFF201148, 0xFF201148, 0xFF55E7FF, 0xFF55E7FF, 0xFF201148),
        KwhaleCatalogTheme(10, "neth3me", "community", false, 10, 0xFF191522, 0xFF191522, 0xFFFFFFFF, 0xFF8E8E93, 0xFF191522),
        KwhaleCatalogTheme(11, "Milkshake", "dark", false, 2, 0xFF19191A, 0xFF2C2D2E, 0xFFE1E3E6, 0xFF76787A, 0xFF2C2D2E),
        KwhaleCatalogTheme(12, "Wayne", "community", false, 28, 0xFF202832, 0xFF202832, 0xFFFFFFFF, 0xFF8E8E93, 0xFF202832),
        KwhaleCatalogTheme(13, "Solarized (светлая)", "light", true, 42, 0xFFFDF6E3, 0xFFEEE8D5, 0xFF586E75, 0xFF93A1A1, 0xFFEEE8D5),
        KwhaleCatalogTheme(14, "Solarized (темная)", "dark", false, 47, 0xFF002B36, 0xFF073642, 0xFF839496, 0xFF586E75, 0xFF073642),
        KwhaleCatalogTheme(15, "Flax", "dark", false, 28, 0xFF21263A, 0xFF2B2F48, 0xFFFFFFFF, 0xFF8E8E93, 0xFF2B2F48),
        KwhaleCatalogTheme(16, "Северное сияние", "dark", false, 50, 0xFF001B12, 0xFF142C24, 0xFFD0E0CF, 0xFF667670, 0xFF142C24),
        KwhaleCatalogTheme(17, "Ukrrooter", "community", false, 51, 0xFF9600FF, 0xFF9600FF, 0xFF00FF00, 0xFF00FF00, 0xFF9600FF),
        KwhaleCatalogTheme(18, "Revz", "community", false, 52, 0xFF3C3C3C, 0xFF3C3C3C, 0xFFFFFFFF, 0xFF8E8E93, 0xFF3C3C3C),
        KwhaleCatalogTheme(19, "Ubuntu", "community", false, 54, 0xFF2C001E, 0xFF2D132C, 0xFFFFFFFF, 0xFFAEA79F, 0xFF2D132C),
        KwhaleCatalogTheme(20, "Вечер", "dark", false, 70, 0xFF20232F, 0xFF2A2D39, 0xFFFFFFFF, 0xFF7F818F, 0xFF20232F),
        KwhaleCatalogTheme(21, "Рассвет", "light", true, 71, 0xFFFDFAF0, 0xFFF4EFE5, 0xFF4E312F, 0xFFAB9F94, 0xFFFDFAF0),
        KwhaleCatalogTheme(22, "Ruby", "community", false, 92, 0xFF323641, 0xFF323641, 0xFFFFFFFF, 0xFF8E8E93, 0xFF323641),
        KwhaleCatalogTheme(23, "Moonstone", "community", false, 94, 0xFF370051, 0xFF370051, 0xFFFFFFFF, 0xFF8E8E93, 0xFF370051),
        KwhaleCatalogTheme(24, "Greenland", "community", false, 56, 0xFF282828, 0xFF121212, 0xFFFFFFFF, 0xFF8E8E93, 0xFF282828),
        KwhaleCatalogTheme(25, "Rena Mint", "community", false, 58, 0xFF313745, 0xFF212735, 0xFFFFFFFF, 0xFF8E8E93, 0xFF313745),
        KwhaleCatalogTheme(26, "Jellygram", "dark", false, 95, 0xFF0F101E, 0xFF181929, 0xFFFFFFFF, 0xFF8E8E93, 0xFF0F101E),
        KwhaleCatalogTheme(27, "Mineshaft Gold", "dark", false, 96, 0xFF1A1A1A, 0xFF1F1F1F, 0xFFFFFFFF, 0xFF8E8E93, 0xFF1A1A1A),
        KwhaleCatalogTheme(28, "Midnight Firewatch", "dark", false, 97, 0xFF031113, 0xFF0D1B1D, 0xFFFFFFFF, 0xFF8E8E93, 0xFF031113),
        KwhaleCatalogTheme(29, "Nord: Темный", "dark", false, 98, 0xFF2E3440, 0xFF3B4252, 0xFFECEFF4, 0xFFE5E9F0, 0xFF3B4252),
        KwhaleCatalogTheme(30, "Nord: Светлый", "light", true, 98, 0xFFECEFF4, 0xFFE5E9F0, 0xFF2E3440, 0xFF3B4252, 0xFFE5E9F0),
        KwhaleCatalogTheme(31, "Gruvbox: Темный", "community", false, 112, 0xFF3C3836, 0xFF282828, 0xFFEBDBB2, 0xFFD5C4A1, 0xFF282828),
        KwhaleCatalogTheme(32, "Gruvbox: Светлый", "community", true, 111, 0xFFFBF1C7, 0xFFEBDBB2, 0xFF3C3836, 0xFF504945, 0xFFEBDBB2),
        KwhaleCatalogTheme(33, "Relaunch", "dark", false, 115, 0xFF000000, 0xFF121212, 0xFFFFFFFF, 0xFF8E8E93, 0xFF000000),
        KwhaleCatalogTheme(34, "Steam Green", "dark", false, 120, 0xFF353A27, 0xFF3E452C, 0xFFFFFFFF, 0xFF8E8E93, 0xFF3E452C),
        KwhaleCatalogTheme(35, "Adwaita (light)", "light", true, 121, 0xFFFAFAFA, 0xFFEBEBEB, 0xFF111111, 0xFF6B6B70, 0xFFEBEBEB),
        KwhaleCatalogTheme(36, "Adwaita (dark)", "dark", false, 122, 0xFF242424, 0xFF303030, 0xFFFFFFFF, 0xFF8E8E93, 0xFF303030),
    )

    fun byId(id: Int): KwhaleCatalogTheme = all.firstOrNull { it.id == id } ?: all.first()
}
