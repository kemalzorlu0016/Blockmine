package com.example.data

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "BlockMiner",
    val titleBadge: String = "Usta Mimar",
    val skinPreset: String = "diamond_knight",
    val hairColorHex: String = "#3B271A",
    val tunicColorHex: String = "#00B4D8",
    val pantsColorHex: String = "#1D3557",
    val capeStyle: String = "royal_gold",
    val level: Long = 1L,
    val xp: Long = 100L,
    val coins: Long = 250L,
    val gems: Long = 25L,
    val dailyStreak: Long = 1L,
    val lastClaimDay: Long = 0L,
    val blocksPlaced: Long = 0L,
    val blocksMined: Long = 0L,
    val templatesBuilt: Long = 0L,
    val languageCode: String = "tr",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendName: String = "",
    val friendTag: String = "#TR01",
    val status: String = "online",
    val serverRoom: String = "Anadolu Survival #1",
    val skinPreset: String = "emerald_explorer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServerRoom(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val roomName: String = "Anadolu Survival #1",
    val gameMode: String = "creative",
    val region: String = "EU-TR",
    val playerCount: Long = 1L,
    val maxPlayers: Long = 16L,
    val memberUids: List<String> = emptyList(),
    val lastChatMessage: String = "BLOCKMİNE sunucusuna hoş geldiniz!",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    TR("tr", "Türkçe", "🇹🇷"),
    EN("en", "English", "🇬🇧"),
    DE("de", "Deutsch", "🇩🇪"),
    ES("es", "Español", "🇪🇸"),
    JA("ja", "日本語", "🇯🇵");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TR
    }
}

data class VoxelBlockType(
    val id: Int,
    val key: String,
    val nameTr: String,
    val nameEn: String,
    val topColor: Long,
    val sideColor: Long,
    val accentColor: Long,
    val category: String,
    val isTranslucent: Boolean = false,
    val isEmissive: Boolean = false
) {
    fun localizedName(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> nameTr
        else -> nameEn
    }
}

data class VoxelPlacement(
    val dx: Int,
    val dy: Int,
    val dz: Int,
    val blockId: Int
)

data class StructureTemplate(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val iconEmoji: String,
    val blocksCount: Int,
    val rewardXp: Int,
    val placements: List<VoxelPlacement>
) {
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.TR) nameTr else nameEn
    fun localizedDesc(lang: AppLanguage): String = if (lang == AppLanguage.TR) descriptionTr else descriptionEn
}

data class DailyQuestItem(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val descTr: String,
    val descEn: String,
    val target: Int,
    val current: Int,
    val rewardXp: Int,
    val rewardCoins: Int,
    val rewardGems: Int,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean get() = current >= target
}

data class DailyRewardItem(
    val day: Int,
    val titleTr: String,
    val titleEn: String,
    val coins: Int,
    val gems: Int,
    val specialItem: String? = null
)

data class LeaderboardEntryItem(
    val rank: Int,
    val username: String,
    val titleBadge: String,
    val level: Int,
    val score: Long,
    val blocksBuilt: Long,
    val isCurrentUser: Boolean = false,
    val skinColorHex: String = "#00B4D8"
)

data class NotificationReport(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val summaryTr: String,
    val summaryEn: String,
    val metricLabel: String,
    val timestampLabel: String,
    val category: String, // "analytics", "reward", "multiplayer", "system"
    val isUnread: Boolean = true
)

enum class WeatherPreset(val labelTr: String, val labelEn: String, val skyTop: Long, val skyBottom: Long) {
    SUNNY("Güneşli HD", "Sunny HD", 0xFF4EA8DE, 0xFF90E0EF),
    SUNSET("Kızıl Gün Batımı", "Golden Sunset", 0xFF6A040F, 0xFFF48C06),
    NIGHT("Yıldızlı Gece", "Starry Night", 0xFF0B132B, 0xFF1C2541),
    NEON_AURORA("Kuzey Işıkları", "Aurora Glow", 0xFF10002B, 0xFF00F5D4)
}

enum class GraphicsQuality(val labelTr: String, val labelEn: String, val renderRadius: Int, val enableShadows: Boolean, val enableAO: Boolean) {
    PERFORMANCE("Performans (60 FPS)", "Performance (60 FPS)", 5, false, false),
    BALANCED("Dengeli HD", "Balanced HD", 7, true, false),
    ULTRA_100X("Ultra 100x HD Voxel", "Ultra 100x HD Voxel", 9, true, true)
}

data class GraphicsAndUiConfig(
    val quality: GraphicsQuality = GraphicsQuality.ULTRA_100X,
    val weather: WeatherPreset = WeatherPreset.SUNNY,
    val fov: Float = 75f,
    val hudOpacity: Float = 0.92f,
    val buttonScale: Float = 1.0f,
    val leftHandedControls: Boolean = false,
    val sfxVolume: Float = 0.85f,
    val musicVolume: Float = 0.60f,
    val showFpsGraph: Boolean = true,
    val showMinimap: Boolean = true
)

object BlockCatalog {
    val allBlocks: List<VoxelBlockType> = listOf(
        VoxelBlockType(1, "grass", "Çimen Bloğu", "Grass Block", 0xFF5EBD3E, 0xFF866043, 0xFF47892C, "Doğa"),
        VoxelBlockType(2, "dirt", "Toprak", "Dirt", 0xFF866043, 0xFF735137, 0xFF99704E, "Doğa"),
        VoxelBlockType(3, "stone", "Taş", "Stone", 0xFF7D7D7D, 0xFF686868, 0xFF949494, "İnşa"),
        VoxelBlockType(4, "cobble", "Kırık Taş", "Cobblestone", 0xFF636363, 0xFF4F4F4F, 0xFF808080, "İnşa"),
        VoxelBlockType(5, "oak_log", "Meşe Odunu", "Oak Log", 0xFFA48453, 0xFF5C4326, 0xFF422F1A, "Doğa"),
        VoxelBlockType(6, "oak_planks", "Meşe Tahtası", "Oak Planks", 0xFFB8945F, 0xFFA17E4B, 0xFFCBA670, "İnşa"),
        VoxelBlockType(7, "leaves", "Ağaç Yaprağı", "Oak Leaves", 0xFF3A7D22, 0xFF2D6319, 0xFF52A333, "Doğa"),
        VoxelBlockType(8, "sand", "Kum", "Sand", 0xFFE3DB9A, 0xFFD1C782, 0xFFEDE6B1, "Doğa"),
        VoxelBlockType(9, "sandstone", "Kumtaşı", "Sandstone", 0xFFD9CE8B, 0xFFC4B770, 0xFFE8DF9E, "İnşa"),
        VoxelBlockType(10, "glass", "Kristal Cam", "Crystal Glass", 0xFFA8E6CF, 0xFF7FDBDA, 0xFFE0F7FA, "Dekor", isTranslucent = true),
        VoxelBlockType(11, "brick", "Kırmızı Tuğla", "Bricks", 0xFFA3483B, 0xFF8C392E, 0xFFC76354, "İnşa"),
        VoxelBlockType(12, "gold", "Altın Bloğu", "Gold Block", 0xFFFFD166, 0xFFF4A261, 0xFFFFEA9F, "Değerli"),
        VoxelBlockType(13, "diamond", "Elmas Cevheri", "Diamond Ore", 0xFF00F5D4, 0xFF6C757D, 0xFF72EFDD, "Değerli", isEmissive = true),
        VoxelBlockType(14, "emerald", "Zümrüt Bloğu", "Emerald Block", 0xFF06D6A0, 0xFF049A73, 0xFF6FFFE9, "Değerli", isEmissive = true),
        VoxelBlockType(15, "obsidian", "Obsidyen", "Obsidian", 0xFF240046, 0xFF10002B, 0xFF5A189A, "Değerli"),
        VoxelBlockType(16, "glowstone", "Işık Taşı", "Glowstone", 0xFFFFBE0B, 0xFFFB5607, 0xFFFFE57F, "Dekor", isEmissive = true),
        VoxelBlockType(17, "quartz", "Saf Kuvars", "Quartz Block", 0xFFF8F9FA, 0xFFE9ECEF, 0xFFFFFFFF, "İnşa"),
        VoxelBlockType(18, "water", "Berrak Su", "Clear Water", 0xFF0096C7, 0xFF0077B6, 0xFF48CAE4, "Doğa", isTranslucent = true),
        VoxelBlockType(19, "lava", "Magma Lav", "Magma Lava", 0xFFFF5400, 0xFFD00000, 0xFFFFBD00, "Doğa", isEmissive = true),
        VoxelBlockType(20, "tnt", "Dinamit (TNT)", "TNT Block", 0xFFE63946, 0xFFD62828, 0xFFF1FAEE, "Özel"),
        VoxelBlockType(21, "redstone", "Kızıltaş Bloğu", "Redstone Block", 0xFFD90429, 0xFF9B001A, 0xFFFF4D6D, "Özel", isEmissive = true),
        VoxelBlockType(22, "portal", "Nether Portalı", "Nether Portal", 0xFF9D4EDD, 0xFF7B2CBF, 0xFFE0AAFF, "Özel", isTranslucent = true, isEmissive = true),
        VoxelBlockType(23, "amethyst", "Ametist Kristali", "Amethyst Block", 0xFFC77DFF, 0xFF9D4EDD, 0xFFE0AAFF, "Değerli"),
        VoxelBlockType(24, "snow", "Kar Bloğu", "Snow Block", 0xFFEDF2F4, 0xFFD8E2DC, 0xFFFFFFFF, "Doğa")
    )

    fun byId(id: Int): VoxelBlockType = allBlocks.firstOrNull { it.id == id } ?: allBlocks.first()

    val templates: List<StructureTemplate> by lazy {
        listOf(
            buildVillagerHouseTemplate(),
            buildWatchtowerTemplate(),
            buildPyramidTemplate(),
            buildNetherPortalTemplate(),
            buildOakTreeTemplate(),
            buildRoyalCastleTemplate(),
            buildModernVillaTemplate()
        )
    }

    private fun buildVillagerHouseTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        // 5x5 Cobblestone foundation & walls with oak corner pillars, windows, and roof
        for (x in -2..2) {
            for (z in -2..2) {
                list.add(VoxelPlacement(x, 0, z, 4)) // Cobblestone floor
                if (x == -2 || x == 2 || z == -2 || z == 2) {
                    val isCorner = (x == -2 || x == 2) && (z == -2 || z == 2)
                    val isDoor = (z == -2 && x == 0)
                    for (y in 1..3) {
                        if (isDoor && y <= 2) continue
                        val blockId = when {
                            isCorner -> 5 // Oak log
                            y == 2 && (x == 0 || z == 0) -> 10 // Glass window
                            else -> 6 // Oak planks
                        }
                        list.add(VoxelPlacement(x, y, z, blockId))
                    }
                }
            }
        }
        // Pitched roof
        for (x in -2..2) {
            for (z in -2..2) {
                list.add(VoxelPlacement(x, 4, z, 5))
            }
        }
        for (x in -1..1) {
            for (z in -1..1) {
                list.add(VoxelPlacement(x, 5, z, 11))
            }
        }
        list.add(VoxelPlacement(0, 3, 0, 16)) // Glowstone chandelier
        return StructureTemplate(
            id = "villager_house",
            nameTr = "Köylü Evi",
            nameEn = "Villager House",
            descriptionTr = "Meşe kütükleri, cam pencereler, taş temel ve aydınlatmalı klasik Minecraft evi.",
            descriptionEn = "Classic villager cottage with oak logs, glass windows, cobblestone base, and lighting.",
            iconEmoji = "🏡",
            blocksCount = list.size,
            rewardXp = 120,
            placements = list
        )
    }

    private fun buildWatchtowerTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (y in 0..6) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (y == 0 || x == -1 || x == 1 || z == -1 || z == 1) {
                        if (y in 1..2 && x == 0 && z == -1) continue // entrance
                        list.add(VoxelPlacement(x, y, z, if (y % 2 == 0) 3 else 4))
                    }
                }
            }
        }
        // Top battlements
        for (x in -2..2) {
            for (z in -2..2) {
                list.add(VoxelPlacement(x, 7, z, 4))
                if ((x == -2 || x == 2 || z == -2 || z == 2) && (x + z) % 2 == 0) {
                    list.add(VoxelPlacement(x, 8, z, 3))
                }
            }
        }
        list.add(VoxelPlacement(0, 8, 0, 16)) // Beacon light
        return StructureTemplate(
            id = "watchtower",
            nameTr = "Gözetleme Kulesi",
            nameEn = "Watchtower",
            descriptionTr = "Sur mazgalları ve tepe feneriyle yüksek savunma ve keşif kulesi.",
            descriptionEn = "Tall stone defense tower with battlements and beacon lantern.",
            iconEmoji = "🗼",
            blocksCount = list.size,
            rewardXp = 150,
            placements = list
        )
    }

    private fun buildPyramidTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (layer in 0..3) {
            val radius = 3 - layer
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    val blockId = if (layer == 3) 12 else 9 // Gold tip on sandstone pyramid
                    list.add(VoxelPlacement(x, layer, z, blockId))
                }
            }
        }
        return StructureTemplate(
            id = "desert_pyramid",
            nameTr = "Antik Piramit",
            nameEn = "Ancient Pyramid",
            descriptionTr = "Altın zirveli basamaklı kumtaşı firavun piramidi.",
            descriptionEn = "Stepped sandstone pharaoh pyramid crowned with solid gold.",
            iconEmoji = "🏜️",
            blocksCount = list.size,
            rewardXp = 180,
            placements = list
        )
    }

    private fun buildNetherPortalTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (x in -1..2) {
            for (y in 0..4) {
                val isFrame = (x == -1 || x == 2 || y == 0 || y == 4)
                list.add(VoxelPlacement(x, y, 0, if (isFrame) 15 else 22))
            }
        }
        return StructureTemplate(
            id = "nether_portal",
            nameTr = "Nether Portalı",
            nameEn = "Nether Portal",
            descriptionTr = "Obsidyen çerçeveli ve parlayan mor enerji çekirdekli boyut kapısı.",
            descriptionEn = "Obsidian dimensional gateway with glowing purple portal core.",
            iconEmoji = "🔮",
            blocksCount = list.size,
            rewardXp = 140,
            placements = list
        )
    }

    private fun buildOakTreeTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (y in 0..3) {
            list.add(VoxelPlacement(0, y, 0, 5))
        }
        for (y in 3..5) {
            val r = if (y == 5) 1 else 2
            for (x in -r..r) {
                for (z in -r..r) {
                    if (x == 0 && z == 0 && y <= 3) continue
                    list.add(VoxelPlacement(x, y, z, 7))
                }
            }
        }
        return StructureTemplate(
            id = "oak_tree",
            nameTr = "Ulu Meşe Ağacı",
            nameEn = "Grand Oak Tree",
            descriptionTr = "Doğal gövdeli ve gür yaprak taçlı hazır ağaç şablonu.",
            descriptionEn = "Instant lush oak tree with trunk and dense leaf canopy.",
            iconEmoji = "🌳",
            blocksCount = list.size,
            rewardXp = 75,
            placements = list
        )
    }

    private fun buildRoyalCastleTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (x in -3..3) {
            for (z in -3..3) {
                list.add(VoxelPlacement(x, 0, z, 3))
                val isOuter = (x == -3 || x == 3 || z == -3 || z == 3)
                val isCorner = (kotlin.math.abs(x) >= 2 && kotlin.math.abs(z) >= 2)
                if (isOuter) {
                    for (y in 1..3) {
                        if (z == -3 && x == 0 && y <= 2) continue
                        list.add(VoxelPlacement(x, y, z, 4))
                    }
                    if ((x + z) % 2 == 0) {
                        list.add(VoxelPlacement(x, 4, z, 3))
                    }
                }
                if (isCorner) {
                    list.add(VoxelPlacement(x, 4, z, 4))
                    list.add(VoxelPlacement(x, 5, z, 12))
                }
            }
        }
        list.add(VoxelPlacement(0, 1, 0, 14)) // Emerald throne center
        return StructureTemplate(
            id = "royal_castle",
            nameTr = "Kraliyet Kalesi",
            nameEn = "Royal Castle",
            descriptionTr = "4 köşe kulesi, altın burçları ve zümrüt tahtıyla görkemli kale.",
            descriptionEn = "Fortified royal castle with 4 corner towers, battlements, and emerald core.",
            iconEmoji = "🏰",
            blocksCount = list.size,
            rewardXp = 250,
            placements = list
        )
    }

    private fun buildModernVillaTemplate(): StructureTemplate {
        val list = mutableListOf<VoxelPlacement>()
        for (x in -2..2) {
            for (z in -2..2) {
                list.add(VoxelPlacement(x, 0, z, 17)) // Quartz floor
                list.add(VoxelPlacement(x, 4, z, 17)) // Quartz flat roof
                if (x == -2 || x == 2 || z == -2 || z == 2) {
                    for (y in 1..3) {
                        if (z == -2 && x == 0 && y <= 2) continue
                        val b = if (y == 2) 10 else 17
                        list.add(VoxelPlacement(x, y, z, b))
                    }
                }
            }
        }
        // Mini pool in front
        for (x in -1..1) {
            list.add(VoxelPlacement(x, 0, -3, 18))
        }
        return StructureTemplate(
            id = "modern_villa",
            nameTr = "Modern Kuvars Villa",
            nameEn = "Modern Quartz Villa",
            descriptionTr = "Saf kuvars duvarlar, panoramik kristal camlar ve ön yüzme havuzu.",
            descriptionEn = "Sleek quartz architecture with panoramic crystal glass and front pool.",
            iconEmoji = "🏢",
            blocksCount = list.size,
            rewardXp = 200,
            placements = list
        )
    }
}
