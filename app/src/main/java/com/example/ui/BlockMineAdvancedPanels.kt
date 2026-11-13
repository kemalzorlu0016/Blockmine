package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.data.AppLanguage
import com.example.data.GraphicsAndUiConfig
import com.example.data.GraphicsQuality
import com.example.data.NotificationReport
import com.example.data.UserProfile
import com.example.data.WeatherPreset

/**
 * Character Skin & User Profile Customizer Modal
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCustomizerModal(
    profile: UserProfile,
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var titleBadge by remember(profile.titleBadge) { mutableStateOf(profile.titleBadge) }
    var skinPreset by remember(profile.skinPreset) { mutableStateOf(profile.skinPreset) }
    var hairHex by remember(profile.hairColorHex) { mutableStateOf(profile.hairColorHex) }
    var tunicHex by remember(profile.tunicColorHex) { mutableStateOf(profile.tunicColorHex) }
    var pantsHex by remember(profile.pantsColorHex) { mutableStateOf(profile.pantsColorHex) }
    var capeStyle by remember(profile.capeStyle) { mutableStateOf(profile.capeStyle) }

    val badges = listOf("Usta Mimar", "Elmas Şövalye", "Kızıltaş Mühendisi", "Nether Kaşifi", "Kraliyet Muhafızı")
    val tunicColors = listOf("#00B4D8" to "Elmas Mavisi", "#06D6A0" to "Zümrüt Yeşili", "#FFD166" to "Altın Zırh", "#E63946" to "Kızıltaş", "#9D4EDD" to "Ametist")
    val capeOptions = listOf("royal_gold" to "👑 Kraliyet Altın Pelerin", "emerald_wings" to "🦅 Zümrüt Kanat", "nether_flame" to "🔥 Nether Alevi")

    ModalContainer(
        title = "🎨 Karakter & Kullanıcı Profili Özelleştirme",
        subtitle = "3B Voxel görünümünü, zırh rengini, pelerinini ve unvanını kişiselleştir!",
        onClose = onClose,
        tag = "character_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live 3D Voxel Character Avatar Preview + Profile Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val parsedTunic = parseSafeColor(tunicHex, Color(0xFF00B4D8))
                    val parsedHair = parseSafeColor(hairHex, Color(0xFF3B271A))
                    val parsedPants = parseSafeColor(pantsHex, Color(0xFF1D3557))
                    Canvas(modifier = Modifier.size(92.dp)) {
                        val w = size.width
                        val h = size.height
                        // Cape
                        drawRect(
                            color = Color(0xFFFFBE0B),
                            topLeft = Offset(w * 0.22f, h * 0.30f),
                            size = Size(w * 0.56f, h * 0.52f)
                        )
                        // Head
                        drawRect(
                            color = Color(0xFFF4A261),
                            topLeft = Offset(w * 0.30f, h * 0.08f),
                            size = Size(w * 0.40f, h * 0.26f)
                        )
                        // Hair
                        drawRect(
                            color = parsedHair,
                            topLeft = Offset(w * 0.28f, h * 0.06f),
                            size = Size(w * 0.44f, h * 0.09f)
                        )
                        // Tunic / Armor
                        drawRect(
                            color = parsedTunic,
                            topLeft = Offset(w * 0.26f, h * 0.34f),
                            size = Size(w * 0.48f, h * 0.34f)
                        )
                        // Pants
                        drawRect(
                            color = parsedPants,
                            topLeft = Offset(w * 0.30f, h * 0.68f),
                            size = Size(w * 0.40f, h * 0.26f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Oyuncu Adı") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_username"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Text(
                        text = "Seviye ${profile.level} • ${profile.xp} XP • Unvan: $titleBadge",
                        color = Color(0xFFFFD166),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text("🏅 Oyuncu Unvanı Seçin:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                badges.forEach { b ->
                    val selected = b == titleBadge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF5EBD3E) else Color(0xFF1E293B))
                            .clickable { titleBadge = b }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = b,
                            color = if (selected) Color(0xFF081C15) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text("🛡️ Zırh / Tunik Rengi:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tunicColors.forEach { (hex, label) ->
                    val selected = hex.equals(tunicHex, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF334155) else Color(0xFF1E293B))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Color(0xFFFFF254) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { tunicHex = hex }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(parseSafeColor(hex, Color.Cyan), CircleShape)
                        )
                        Text(label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Text("🧣 Pelerin Stili:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capeOptions.forEach { (id, label) ->
                    val selected = id == capeStyle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF8B5CF6) else Color(0xFF1E293B))
                            .clickable { capeStyle = id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    onSave(username, titleBadge, skinPreset, hairHex, tunicHex, pantsHex, capeStyle)
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_character"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 Profili & Karakteri Buluta Kaydet", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Detailed Statistics & Telemetry Analytics Dashboard Modal
 */
@Composable
fun AnalyticsDashboardModal(
    profile: UserProfile,
    fpsHistory: List<Int>,
    totalWorldBlocks: Int,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "📊 Detaylı İstatistik & Gelişim Analiz Paneli",
        subtitle = "Gerçek zamanlı kare hızı (FPS), inşa verimliliği ve oyuncu gelişim metrikleri",
        onClose = onClose,
        tag = "analytics_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryCard("🧱 Koyulan Blok", "${profile.blocksPlaced}", "+15 XP / blok", Color(0xFF10B981), Modifier.weight(1f))
                MetricSummaryCard("⛏️ Kırılan Blok", "${profile.blocksMined}", "+10 XP / blok", Color(0xFFF59E0B), Modifier.weight(1f))
                MetricSummaryCard("🏰 Kurulan Şablon", "${profile.templatesBuilt}", "Mimari Skor", Color(0xFF38BDF8), Modifier.weight(1f))
            }

            // Visual Bar Chart of Activity Breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📈 Oyuncu Gelişim & Performans Grafiği (60 FPS Motor Telemetrisi)",
                        color = Color(0xFF00F5D4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val barCount = fpsHistory.size
                        val barWidth = (size.width / (barCount * 1.6f)).coerceAtLeast(10f)
                        val spacing = (size.width - barWidth * barCount) / (barCount + 1).coerceAtLeast(1)
                        fpsHistory.forEachIndexed { idx, fps ->
                            val ratio = (fps / 60f).coerceIn(0.2f, 1f)
                            val bh = size.height * ratio
                            val x = spacing + idx * (barWidth + spacing)
                            drawRect(
                                color = Color(0xFF5EBD3E),
                                topLeft = Offset(x, size.height - bh),
                                size = Size(barWidth, bh)
                            )
                        }
                    }
                    Text(
                        text = "Aktif Sahne Blok Sayısı: $totalWorldBlocks Voxel • Ortalama Kare Hızı: 60.0 FPS • Bellek: Optimize",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
            Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(sub, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

/**
 * High-Resolution Graphics, Audio & Customizable UI Settings Modal
 */
@Composable
fun GraphicsAndUiSettingsModal(
    config: GraphicsAndUiConfig,
    onUpdateConfig: (GraphicsAndUiConfig) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    var musicOn by remember { mutableStateOf(SoundEngine.isMusicEnabled) }

    ModalContainer(
        title = "⚙️ Yüksek Çözünürlüklü Grafik, Ses & Özel Arayüz",
        subtitle = "Cihazınıza göre 100x HD Voxel kalitesini, atmosferi ve kontrolleri özelleştirin",
        onClose = onClose,
        tag = "settings_modal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🖥️ Grafik Kalitesi & Çözünürlük Modu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GraphicsQuality.entries.forEach { q ->
                    val selected = config.quality == q
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF5EBD3E) else Color(0xFF1E293B))
                            .clickable { onUpdateConfig(config.copy(quality = q)) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = q.labelTr,
                            color = if (selected) Color(0xFF081C15) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text("🌤️ Atmosfer & Gökyüzü Aydınlatması:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeatherPreset.entries.forEach { w ->
                    val selected = config.weather == w
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF0284C7) else Color(0xFF1E293B))
                            .clickable { onUpdateConfig(config.copy(weather = w)) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(w.labelTr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "🔭 Kamera Görüş Açısı (FOV: ${config.fov.toInt()}°) & Arayüz Boyutu (${(config.buttonScale * 100).toInt()}%)",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
            Slider(
                value = config.fov,
                onValueChange = { onUpdateConfig(config.copy(fov = it)) },
                valueRange = 60f..105f
            )

            Text(
                text = "🔊 Ses Efektleri Seviyesi (${(config.sfxVolume * 100).toInt()}%)",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
            Slider(
                value = config.sfxVolume,
                onValueChange = { onUpdateConfig(config.copy(sfxVolume = it)) },
                valueRange = 0f..1f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎵 C418 Tarzı Ambient Arka Plan Müziği", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = musicOn,
                    onCheckedChange = {
                        musicOn = it
                        onToggleMusic(it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️ Sağ Üst Canlı 2B Minimap Radar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = config.showMinimap,
                    onCheckedChange = { onUpdateConfig(config.copy(showMinimap = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤚 Solak Kontrol Düzeni (Joystick & Aksiyon Yönü)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = config.leftHandedControls,
                    onCheckedChange = { onUpdateConfig(config.copy(leftHandedControls = it)) }
                )
            }
        }
    }
}

/**
 * Notifications & Detailed Analysis Reports Panel
 */
@Composable
fun NotificationsReportModal(
    lang: AppLanguage,
    reports: List<NotificationReport>,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "🔔 Bildirim & Detaylı Analiz Raporları",
        subtitle = "Oturum performans analizleri, çok oyunculu telemetri ve ödül bildirimleri",
        onClose = onClose,
        tag = "notifications_modal"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 410.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reports, key = { it.id }) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (lang == AppLanguage.TR) rep.titleTr else rep.titleEn,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = rep.timestampLabel,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = if (lang == AppLanguage.TR) rep.summaryTr else rep.summaryEn,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                        Text(
                            text = rep.metricLabel,
                            color = Color(0xFF00F5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Beginner Tutorial Guide Modal
 */
@Composable
fun InteractiveTutorialModal(
    step: Int,
    onNextStep: () -> Unit,
    onClose: () -> Unit
) {
    val steps = listOf(
        Triple(
            "1/4 • 360° Kamera ve Hareket Kontrolü",
            "Ekranın ortasında parmağınızı sürükleyerek 360° etrafınıza bakabilirsiniz. Sol alttaki yön tuşları / joystick ile dünyayı özgürce keşfedin ve 'Zıpla' butonuyla tepelere çıkın.",
            "🧭 Kamera & Hareket"
        ),
        Triple(
            "2/4 • Blok Kırma ve Yerleştirme",
            "Ekranın ortasındaki nişangahın (Crosshair) gösterdiği bloğu '⛏️ Kır' butonuyla kazabilir, '🧱 Koy' butonuyla seçili bloğu anında yerleştirebilirsiniz.",
            "⛏️ Madencilik & İnşa"
        ),
        Triple(
            "3/4 • Sürükle-Bırak Envanter & Hazır Şablonlar",
            "'🎒 Envanter' ekranından 24 farklı HD bloğu hızlı slotlara sürükleyip bırakabilirsiniz. '🏰 Şablon Kur' menüsüyle Köylü Evi, Kale, Piramit ve Nether Portalı'nı tek tıkla inşa edin!",
            "🏰 Şablonlar & Envanter"
        ),
        Triple(
            "4/4 • Online Multiplayer, Arkadaşlar & Günlük Ödüller",
            "Çok oyunculu sunucularda arkadaşlarınızla birlikte keşif yapın, sohbet edin, günlük görevleri tamamlayarak Küresel Liderlik Tablosu'nda zirveye yükselin!",
            "🏆 Çok Oyunculu & Ödül (+100 Altın)"
        )
    )
    val current = steps.getOrElse(step.coerceIn(0, 3)) { steps.first() }

    ModalContainer(
        title = "🎓 Yeni Başlayanlar İçin İnteraktif Eğitim Rehberi",
        subtitle = current.first,
        onClose = onClose,
        tag = "tutorial_modal"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(current.third, color = Color(0xFFFFD166), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(current.second, color = Color.White, fontSize = 14.sp)
                }
            }

            Button(
                onClick = onNextStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_tutorial_next"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (step < 3) "Sonraki Adım ➡️" else "🎓 Eğitimi Tamamla & +100 Altın Kazan!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Multiplayer Live Chat Drawer Modal
 */
@Composable
fun MultiplayerChatModal(
    activeServerName: String,
    messages: List<String>,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    ModalContainer(
        title = "💬 Canlı Sunucu Sohbeti",
        subtitle = activeServerName,
        onClose = onClose,
        tag = "chat_modal"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Mesajınızı yazın...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_chat_message"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSendMessage(input)
                            input = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("btn_send_chat")
                ) {
                    Text("Gönder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseSafeColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
