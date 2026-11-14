package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiBlueprintUiState
import com.example.ai.GeminiChatTurn
import com.example.ai.GeminiChatUiState
import com.example.data.AppLanguage
import com.example.data.BlockCatalog
import com.example.data.StructureTemplate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiAiArchitectModal(
    language: AppLanguage,
    chatState: GeminiChatUiState,
    chatHistory: List<GeminiChatTurn>,
    blueprintState: GeminiBlueprintUiState,
    onAskAssistant: (String) -> Unit,
    onGenerateBlueprint: (String) -> Unit,
    onBuildBlueprintInWorld: (StructureTemplate) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 3B Yapı Mimarı, 1 = AI Rehber Sohbet
    var blueprintPrompt by remember { mutableStateOf("") }
    var chatPrompt by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9030712))
            .padding(14.dp)
            .testTag("gemini_ai_modal"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .heightIn(max = 660.dp)
                .border(2.dp, Color(0xFF8B5CF6), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "✨ BLOCKMİNE Gemini AI Mimar",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Gemini 3.8 Flash • 3B Yapı Tasarımı & Akıllı Rehber",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_close_gemini_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color(0xFFE2E8F0)
                        )
                    }
                }

                // Mode Switcher Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPillButton(
                        label = "🏗️ 3B Yapı Tasarla",
                        selected = selectedTab == 0,
                        accent = Color(0xFF8B5CF6),
                        tag = "tab_gemini_blueprint",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabPillButton(
                        label = "💬 AI Asistan Sohbeti",
                        selected = selectedTab == 1,
                        accent = Color(0xFF06B6D4),
                        tag = "tab_gemini_chat",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "İstediğiniz yapıyı tarif edin; Gemini doğrudan 3B blok planı üretsin ve tek dokunuşla dünyanıza inşa etsin.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )

                        // Quick Preset Prompts
                        val presets = listOf(
                            "💎 Elmas & Kuvars Kristal Tapınağı",
                            "🗼 Işık Taşlı Ortaçağ Gözetleme Kulesi",
                            "🚀 Uzay İstasyonu Kubbesi",
                            "🏡 Cam Pencereli Göl Kulübesi"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEachIndexed { idx, preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(10.dp))
                                        .clickable {
                                            blueprintPrompt = preset
                                            onGenerateBlueprint(preset)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("gemini_preset_$idx")
                                ) {
                                    Text(
                                        text = preset,
                                        color = Color(0xFFE0E7FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = blueprintPrompt,
                            onValueChange = { blueprintPrompt = it },
                            label = { Text("Yapı Tarifi (örn. Altın kubbeli küçük saray)") },
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedLabelColor = Color(0xFFC084FC),
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gemini_blueprint")
                        )

                        Button(
                            onClick = { onGenerateBlueprint(blueprintPrompt) },
                            enabled = blueprintPrompt.isNotBlank() && blueprintState !is GeminiBlueprintUiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_generate_blueprint")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("✨ Gemini ile 3B Yapı Oluştur", fontWeight = FontWeight.ExtraBold)
                        }

                        when (blueprintState) {
                            GeminiBlueprintUiState.Idle -> Unit
                            GeminiBlueprintUiState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFC084FC),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Gemini 3B voxel planını hesaplıyor...",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            is GeminiBlueprintUiState.Error -> {
                                GeminiErrorCard(blueprintState.message, "gemini_blueprint_error")
                            }
                            is GeminiBlueprintUiState.Success -> {
                                val tpl = blueprintState.template
                                val blockCounts = tpl.placements
                                    .groupingBy { it.blockId }
                                    .eachCount()
                                    .entries
                                    .sortedByDescending { it.value }
                                    .take(5)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                        .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                        .testTag("gemini_blueprint_result"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${tpl.iconEmoji} ${tpl.localizedName(language)}",
                                        color = Color(0xFF34D399),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = tpl.localizedDesc(language),
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "🧱 ${tpl.blocksCount} Blok  •  ⭐ +${tpl.rewardXp} XP  •  🪙 +75 Altın",
                                        color = Color(0xFFFFD166),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        blockCounts.forEach { (blockId, count) ->
                                            val block = BlockCatalog.byId(blockId)
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(block.topColor), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${block.localizedName(language)} x$count",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { onBuildBlueprintInWorld(tpl) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_build_ai_blueprint")
                                    ) {
                                        Text(
                                            text = "🏗️ 3B Dünyada İnşa Et",
                                            color = Color(0xFF042F2E),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Streaming AI Architect Assistant Chat
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp, max = 470.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickQuestions = listOf(
                            "Kale savunması için en iyi blok düzeni nedir?",
                            "Kızıltaş ve Işık Taşı ile otomatik fener nasıl yapılır?"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickQuestions.forEachIndexed { idx, q ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(10.dp))
                                        .clickable { onAskAssistant(q) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("gemini_quick_q_$idx")
                                ) {
                                    Text(q, color = Color(0xFFA5F3FC), fontSize = 11.sp)
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF090F1E), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (chatHistory.isEmpty() && chatState is GeminiChatUiState.Idle) {
                                item {
                                    Text(
                                        text = "Merhaba Mimar! BLOCKMİNE blok kombinasyonları, yapı fikirleri ve çok oyunculu taktikler hakkında dilediğini sorabilirsin.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            items(chatHistory) { turn ->
                                val bg = if (turn.isUser) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                val title = if (turn.isUser) "Sen" else "✨ Gemini Mimar"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = Color(0xFF93C5FD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = turn.text,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            when (chatState) {
                                GeminiChatUiState.Loading -> item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF22D3EE),
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text("Gemini düşünüyor...", color = Color(0xFF67E8F9), fontSize = 12.sp)
                                    }
                                }
                                is GeminiChatUiState.Streaming -> item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF16243D), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "✨ Gemini Mimar (yazıyor...)",
                                            color = Color(0xFF22D3EE),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(text = chatState.text, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                is GeminiChatUiState.Error -> item {
                                    GeminiErrorCard(chatState.message, "gemini_chat_error")
                                }
                                else -> Unit
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = chatPrompt,
                                onValueChange = { chatPrompt = it },
                                placeholder = { Text("Gemini'ye sor...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF06B6D4),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_gemini_chat")
                            )
                            Button(
                                onClick = {
                                    val q = chatPrompt
                                    chatPrompt = ""
                                    onAskAssistant(q)
                                },
                                enabled = chatPrompt.isNotBlank() &&
                                    chatState !is GeminiChatUiState.Loading &&
                                    chatState !is GeminiChatUiState.Streaming,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("btn_send_gemini_chat")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else Color(0xFF1E293B))
            .border(1.dp, if (selected) Color.White else Color(0xFF475569), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun GeminiErrorCard(message: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF450A0A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF87171), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag(tag)
    ) {
        Text(
            text = "⚠️ $message",
            color = Color(0xFFFECACA),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
