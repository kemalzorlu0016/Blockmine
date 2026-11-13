package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.BlockCatalog
import com.example.data.DailyQuestItem
import com.example.data.DailyRewardItem
import com.example.data.FriendEntry
import com.example.data.LeaderboardEntryItem
import com.example.data.ServerRoom
import com.example.data.StructureTemplate
import com.example.data.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleScreenModal(
    strings: LocalizedUiStrings,
    profile: UserProfile,
    activeServerName: String,
    currentLanguage: AppLanguage,
    unreadNotificationsCount: Int,
    onStartGame: () -> Unit,
    onOpenModal: (ActiveModal) -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD90A1118),
                        Color(0xB3121E2B),
                        Color(0xE6080D14)
                    )
                )
            )
            .padding(16.dp)
            .testTag("title_screen_modal"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top telemetry + multi-language chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xCC111927), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF5EBD3E), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF5EBD3E), CircleShape)
                    )
                    Text(
                        text = activeServerName,
                        color = Color(0xFFA7F3D0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Multi-language selector bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF5EBD3E) else Color(0xCC1E293B))
                                .clickable { onChangeLanguage(lang) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("lang_chip_${lang.code}")
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.code.uppercase()}",
                                color = if (isSelected) Color(0xFF081C15) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // BLOCKMİNE 3D Hero Logo Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, Color(0xFF5EBD3E), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE6111927))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BLOCKMİNE",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE9F5DB),
                        letterSpacing = 4.sp,
                        modifier = Modifier.testTag("title_logo_blockmine")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1B4332), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF5EBD3E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.subtitleEdition,
                            color = Color(0xFFFFF254),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Player quick summary pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBadge("👤 ${profile.username}", profile.titleBadge, Color(0xFF00F5D4))
                        StatBadge("⭐ Sv. ${profile.level}", "${profile.xp} XP", Color(0xFFFFD166))
                        StatBadge("🪙 ${profile.coins}", "💎 ${profile.gems}", Color(0xFF5EBD3E))
                    }
                }
            }

            // Primary Play Button (Fixes broken button in HTML!)
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("btn_title_play"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D9C42),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "▶️ ${strings.playNow}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Grid of Feature Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                MenuFeatureButton(
                    title = "🎒 ${strings.creativeInventory}",
                    subtitle = "24 HD Blok & Sürükle-Bırak",
                    accent = Color(0xFF10B981),
                    tag = "btn_open_inventory",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.INVENTORY) }
                )
                MenuFeatureButton(
                    title = "🏰 ${strings.templatesMenu}",
                    subtitle = "7 Hazır 3B Yapı Şablonu",
                    accent = Color(0xFFF59E0B),
                    tag = "btn_open_templates",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.TEMPLATES) }
                )
                MenuFeatureButton(
                    title = "🌐 ${strings.multiplayerServers}",
                    subtitle = "Canlı Oda & Ortak Keşif",
                    accent = Color(0xFF3B82F6),
                    tag = "btn_open_servers",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.MULTIPLAYER_SERVERS) }
                )
                MenuFeatureButton(
                    title = "👥 ${strings.friendsList}",
                    subtitle = "Arkadaş Ekle & Odaya Katıl",
                    accent = Color(0xFF06B6D4),
                    tag = "btn_open_friends",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.FRIENDS_LIST) }
                )
                MenuFeatureButton(
                    title = "🎁 ${strings.dailyQuestsAndRewards}",
                    subtitle = "Günlük Görev & 7 Günlük Ödül",
                    accent = Color(0xFFEC4899),
                    tag = "btn_open_quests",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.DAILY_QUESTS_REWARDS) }
                )
                MenuFeatureButton(
                    title = "🏆 ${strings.leaderboard}",
                    subtitle = "Küresel Sıralama & Rekabet",
                    accent = Color(0xFFEAB308),
                    tag = "btn_open_leaderboard",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.LEADERBOARD) }
                )
                MenuFeatureButton(
                    title = "🎨 ${strings.characterAndProfile}",
                    subtitle = "Kostüm, Pelerin & Unvan",
                    accent = Color(0xFF8B5CF6),
                    tag = "btn_open_character",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.CHARACTER_CUSTOMIZER) }
                )
                MenuFeatureButton(
                    title = "📊 ${strings.statsAndAnalytics}",
                    subtitle = "Performans & İnşa Grafikleri",
                    accent = Color(0xFF14B8A6),
                    tag = "btn_open_analytics",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.ANALYTICS_DASHBOARD) }
                )
                MenuFeatureButton(
                    title = "⚙️ ${strings.graphicsAndSettings}",
                    subtitle = "100x HD, Ses & Özel Arayüz",
                    accent = Color(0xFF64748B),
                    tag = "btn_open_settings",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.GRAPHICS_SETTINGS) }
                )
                MenuFeatureButton(
                    title = "🔔 ${strings.notificationsAndReports} ($unreadNotificationsCount)",
                    subtitle = "Detaylı Analiz Raporları",
                    accent = Color(0xFFF97316),
                    tag = "btn_open_notifications",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenModal(ActiveModal.NOTIFICATIONS_PANEL) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenModal(ActiveModal.INTERACTIVE_TUTORIAL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_open_tutorial"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.interactiveTutorial, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_sign_out"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                ) {
                    Text(strings.signOutLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(title: String, subtitle: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Text(text = subtitle, color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}

@Composable
private fun MenuFeatureButton(
    title: String,
    subtitle: String,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick)
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD91E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = accent,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Drag-and-Drop & Quick-Equip Creative Inventory Modal
 */
@Composable
fun InventoryModal(
    strings: LocalizedUiStrings,
    lang: AppLanguage,
    hotbarBlockIds: List<Int>,
    selectedSlot: Int,
    draggedBlockId: Int?,
    onSelectSlot: (Int) -> Unit,
    onAssignBlockToSlot: (Int, Int) -> Unit,
    onStartDragBlock: (Int?) -> Unit,
    onDropOnSlot: (Int) -> Unit,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "🎒 ${strings.creativeInventory}",
        subtitle = strings.dragDropHint,
        onClose = onClose,
        tag = "inventory_modal"
    ) {
        // Active Drag Indicator
        if (draggedBlockId != null) {
            val draggedBlock = BlockCatalog.byId(draggedBlockId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3A8A), RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFFFF254), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "✋ Taşınan Blok: ${draggedBlock.localizedName(lang)} — Aşağıdaki bir Hızlı Slota dokunarak bırakın!",
                    color = Color(0xFFFFF254),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onStartDragBlock(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("İptal", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 8-Slot Target Hotbar for Drag & Drop
        Text(
            text = "Hızlı Envanter Barı (Hedef Slota Dokunun veya Sürükleyip Bırakın):",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            hotbarBlockIds.forEachIndexed { idx, blockId ->
                val block = BlockCatalog.byId(blockId)
                val isSelected = idx == selectedSlot
                val isDropTarget = draggedBlockId != null
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF334155))
                        .border(
                            width = if (isSelected || isDropTarget) 2.5.dp else 1.dp,
                            color = when {
                                isDropTarget -> Color(0xFF00F5D4)
                                isSelected -> Color(0xFFFFF254)
                                else -> Color(0xFF64748B)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (draggedBlockId != null) {
                                onDropOnSlot(idx)
                            } else {
                                onSelectSlot(idx)
                            }
                        }
                        .testTag("inv_hotbar_slot_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(block.topColor), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(block.sideColor), RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "${idx + 1}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 24 Block Catalog Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BlockCatalog.allBlocks, key = { it.id }) { block ->
                val isBeingDragged = draggedBlockId == block.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isBeingDragged) 2.5.dp else 1.dp,
                            color = if (isBeingDragged) Color(0xFFFFF254) else Color(0xFF475569),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (draggedBlockId == null) {
                                onAssignBlockToSlot(selectedSlot, block.id)
                            } else {
                                onStartDragBlock(block.id)
                            }
                        }
                        .pointerInput(block.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDragBlock(block.id) },
                                onDragEnd = {},
                                onDragCancel = {},
                                onDrag = { change, _ -> change.consume() }
                            )
                        }
                        .testTag("catalog_block_${block.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Isometric 2-tone block preview
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(block.sideColor))
                                .border(1.5.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .background(Color(block.topColor))
                            )
                        }
                        Text(
                            text = block.localizedName(lang),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sürükle / Seç",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            modifier = Modifier.clickable { onStartDragBlock(block.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7 Minecraft Structural Templates Modal
 */
@Composable
fun StructureTemplatesModal(
    strings: LocalizedUiStrings,
    lang: AppLanguage,
    templates: List<StructureTemplate>,
    onBuildTemplate: (StructureTemplate) -> Unit,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "🏰 ${strings.templatesMenu}",
        subtitle = "Tek dokunuşla 3B dünyanıza hazır Minecraft mimari şablonları inşa edin!",
        onClose = onClose,
        tag = "templates_modal"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(templates, key = { it.id }) { tpl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = tpl.iconEmoji, fontSize = 28.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = tpl.localizedName(lang),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = tpl.localizedDesc(lang),
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "🧱 ${tpl.blocksCount} Blok | ⭐ +${tpl.rewardXp} XP Ödül",
                                    color = Color(0xFFFFD166),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onBuildTemplate(tpl) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("build_template_${tpl.id}")
                        ) {
                            Text("İnşa Et", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Online Multiplayer Server Browser & Creator Modal
 */
@Composable
fun MultiplayerServersModal(
    activeServerName: String,
    serversState: UiState<List<ServerRoom>>,
    onJoinPresetServer: (String) -> Unit,
    onCreateServer: (String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var newRoomName by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("creative") }
    var selectedRegion by remember { mutableStateOf("EU-TR") }

    val featuredPublicServers = remember {
        listOf(
            Triple("Anadolu Survival #1 (EU-TR)", "Survival & Ortak Keşif", "14/16 Oyuncu 🟢"),
            Triple("Yaratıcı Mega İnşa (EU-TR)", "Sınırsız Blok & Şablonlar", "11/16 Oyuncu 🟢"),
            Triple("Gökyüzü Adaları SkyBlock (GLOBAL)", "Takım Adası & Rekabet", "9/16 Oyuncu 🟢"),
            Triple("Kızıltaş Mühendislik (EU-DE)", "Otomasyon & Mekanik", "6/16 Oyuncu 🟢")
        )
    }

    ModalContainer(
        title = "🌐 Çok Oyunculu Sunucu Merkezi",
        subtitle = "Aktif Sunucu: $activeServerName",
        onClose = onClose,
        tag = "servers_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create custom room box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "➕ Arkadaşlarınla Özel Sunucu Odası Kur (Firestore Bulut)",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newRoomName,
                            onValueChange = { newRoomName = it },
                            placeholder = { Text("Oda adı (ör. Dostlar Diyarı)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_server_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                val name = newRoomName.ifBlank { "Dostlar Sunucusu" }
                                onCreateServer(name, selectedMode, selectedRegion)
                                newRoomName = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.testTag("btn_create_server")
                        ) {
                            Text("Oluştur", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "🔥 Öne Çıkan Küresel & Türkiye Sunucuları:",
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            featuredPublicServers.forEachIndexed { idx, (title, modeDesc, pop) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$modeDesc • $pop", color = Color(0xFFA7F3D0), fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onJoinPresetServer(title) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.testTag("btn_join_server_$idx")
                        ) {
                            Text("Katıl", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (serversState is UiState.Success && serversState.data.isNotEmpty()) {
                Text(
                    text = "☁️ Senin Bulut Sunucuların:",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                serversState.data.forEach { room ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(room.roomName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    "Mod: ${room.gameMode.uppercase()} | Bölge: ${room.region} | Son: ${room.lastChatMessage}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { onJoinPresetServer("${room.roomName} (${room.region})") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Bağlan")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Friends List & Social Invite Modal
 */
@Composable
fun FriendsListModal(
    friendsState: UiState<List<FriendEntry>>,
    onlinePeers: List<OnlinePeerAvatar>,
    onAddFriend: (String, String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onJoinFriendServer: (String) -> Unit,
    onClose: () -> Unit
) {
    var friendNameInput by remember { mutableStateOf("") }
    var friendTagInput by remember { mutableStateOf("#TR42") }

    ModalContainer(
        title = "👥 Arkadaş Listesi & Sosyal Keşif",
        subtitle = "Arkadaşlarını ekle, aktif sunucularına katıl ve birlikte inşa et!",
        onClose = onClose,
        tag = "friends_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = friendNameInput,
                    onValueChange = { friendNameInput = it },
                    placeholder = { Text("Oyuncu Adı (ör. BerkayHD)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_friend_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = friendTagInput,
                    onValueChange = { friendTagInput = it },
                    placeholder = { Text("#TR", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.width(84.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        if (friendNameInput.isNotBlank()) {
                            onAddFriend(friendNameInput, friendTagInput)
                            friendNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("btn_add_friend")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Arkadaş Ekle")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cloud friends
                if (friendsState is UiState.Success) {
                    items(friendsState.data, key = { it.id }) { friend ->
                        FriendRowCard(
                            name = "${friend.friendName} ${friend.friendTag}",
                            status = "Çevrimiçi 🟢 • ${friend.serverRoom}",
                            onJoin = { onJoinFriendServer(friend.serverRoom) },
                            onDelete = { onRemoveFriend(friend.id) }
                        )
                    }
                }
                // Active server companions
                items(onlinePeers, key = { it.id }) { peer ->
                    FriendRowCard(
                        name = "${peer.name} (${peer.titleBadge})",
                        status = "${peer.statusText} • Anadolu Survival #1",
                        onJoin = { onJoinFriendServer("Anadolu Survival #1 (EU-TR)") },
                        onDelete = null
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRowCard(
    name: String,
    status: String,
    onJoin: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(status, color = Color(0xFF6EE7B7), fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Yanına Git", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFF87171))
                    }
                }
            }
        }
    }
}

/**
 * Daily Quests & 7-Day Loyalty Reward System Modal
 */
@Composable
fun DailyQuestsAndRewardsModal(
    strings: LocalizedUiStrings,
    lang: AppLanguage,
    quests: List<DailyQuestItem>,
    rewardsSchedule: List<DailyRewardItem>,
    claimedDays: Set<Int>,
    onClaimQuest: (String) -> Unit,
    onClaimDailyReward: (Int) -> Unit,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "🎁 ${strings.dailyQuestsAndRewards}",
        subtitle = "Her gün giriş yap, görevleri tamamla ve efsanevi ödülleri topla!",
        onClose = onClose,
        tag = "quests_rewards_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📅 7 Günlük Sadakat Ödül Takvimi:",
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rewardsSchedule.take(4).forEach { item ->
                    val isClaimed = claimedDays.contains(item.day)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.5.dp,
                                if (isClaimed) Color(0xFF10B981) else Color(0xFFF59E0B),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { if (!isClaimed) onClaimDailyReward(item.day) }
                            .testTag("daily_reward_day_${item.day}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${item.day}. GÜN", color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("🪙 ${item.coins}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("💎 ${item.gems}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            Text(
                                text = if (isClaimed) "✅ Alındı" else "🎁 Al",
                                color = if (isClaimed) Color(0xFF10B981) else Color(0xFFFFF254),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Text(
                text = "⚔️ Aktif Günlük Görev Listesi:",
                color = Color(0xFF00F5D4),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )

            quests.forEach { q ->
                val progressRatio = (q.current.toFloat() / q.target.toFloat()).coerceIn(0f, 1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.TR) q.titleTr else q.titleEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (lang == AppLanguage.TR) q.descTr else q.descEn,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { onClaimQuest(q.id) },
                                enabled = q.isCompleted && !q.isClaimed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (q.isClaimed) Color(0xFF475569) else Color(0xFF10B981)
                                ),
                                modifier = Modifier.testTag("claim_quest_${q.id}")
                            ) {
                                Text(
                                    text = when {
                                        q.isClaimed -> strings.claimedLabel
                                        q.isCompleted -> strings.claimReward
                                        else -> "${q.current}/${q.target}"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF5EBD3E),
                            trackColor = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Ödül: +${q.rewardXp} XP • +${q.rewardCoins} Altın • +${q.rewardGems} Elmas",
                            color = Color(0xFFFFD166),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Global Leaderboard Modal
 */
@Composable
fun GlobalLeaderboardModal(
    strings: LocalizedUiStrings,
    entries: List<LeaderboardEntryItem>,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "🏆 ${strings.leaderboard}",
        subtitle = "Dünya genelindeki en iyi BLOCKMİNE mimarları ve madencileri!",
        onClose = onClose,
        tag = "leaderboard_modal"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 410.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.username }) { item ->
                val badgeEmoji = when (item.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#${item.rank}"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (item.isCurrentUser) 2.dp else 1.dp,
                            color = if (item.isCurrentUser) Color(0xFFFFF254) else Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isCurrentUser) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = badgeEmoji,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD166)
                            )
                            Column {
                                Text(
                                    text = if (item.isCurrentUser) "${item.username} (SEN)" else item.username,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${item.titleBadge} • Seviye ${item.level} • ${item.blocksBuilt} Blok",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "${item.score} Puan",
                            color = Color(0xFF00F5D4),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModalContainer(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    tag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060B11))
            .padding(12.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .border(2.5.dp, Color(0xFF5EBD3E), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111927))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color(0xFFDC2626), RoundedCornerShape(10.dp))
                            .size(38.dp)
                            .testTag("btn_close_modal")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}
