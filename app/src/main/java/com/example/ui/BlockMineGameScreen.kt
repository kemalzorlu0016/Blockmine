package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiBlueprintUiState
import com.example.ai.GeminiChatUiState
import com.example.data.BlockCatalog
import com.example.data.StructureTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private class BlockMineWebBridge(
    private val context: Context,
    private val viewModel: BlockMineViewModel,
    private val onSignOut: () -> Unit,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    @JavascriptInterface
    fun generateBlueprint(prompt: String) {
        mainHandler.post {
            viewModel.generateAiBlueprint(prompt)
        }
    }

    @JavascriptInterface
    fun askAssistant(prompt: String) {
        mainHandler.post {
            viewModel.askGeminiAssistant(prompt)
        }
    }

    @JavascriptInterface
    fun signOut() {
        mainHandler.post {
            onSignOut()
        }
    }

    @JavascriptInterface
    fun openPlayStore() {
        mainHandler.post {
            val pkg = context.packageName
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(marketIntent)
            } catch (_: ActivityNotFoundException) {
                runCatching { context.startActivity(webIntent) }
            }
        }
    }
}

private fun StructureTemplate.toWebJson(): String {
    val placementsJson = JSONArray().apply {
        placements.forEach { p ->
            put(
                JSONObject().apply {
                    put("dx", p.dx)
                    put("dy", p.dy)
                    put("dz", p.dz)
                    put("blockId", p.blockId)
                }
            )
        }
    }
    return JSONObject().apply {
        put("id", id)
        put("name", nameTr)
        put("description", descriptionTr)
        put("iconEmoji", iconEmoji)
        put("rewardXp", rewardXp)
        put("placements", placementsJson)
    }.toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockMineGameScreen(
    viewModel: BlockMineViewModel,
    onSignOut: () -> Unit
) {
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(lang) { BlockMineLocalization.strings(lang) }
    val graphicsConfig by viewModel.graphicsConfig.collectAsStateWithLifecycle()
    val worldBlocks by viewModel.worldBlocks.collectAsStateWithLifecycle()
    val playerX by viewModel.playerX.collectAsStateWithLifecycle()
    val playerY by viewModel.playerY.collectAsStateWithLifecycle()
    val playerZ by viewModel.playerZ.collectAsStateWithLifecycle()
    val playerYaw by viewModel.playerYaw.collectAsStateWithLifecycle()
    val playerPitch by viewModel.playerPitch.collectAsStateWithLifecycle()
    val targetedRaycast by viewModel.targetedRaycast.collectAsStateWithLifecycle()
    val hotbarBlockIds by viewModel.hotbarBlockIds.collectAsStateWithLifecycle()
    val selectedSlot by viewModel.selectedHotbarSlot.collectAsStateWithLifecycle()
    val draggedBlockId by viewModel.draggedBlockId.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val activeServerName by viewModel.activeServerName.collectAsStateWithLifecycle()
    val onlinePeers by viewModel.onlinePeers.collectAsStateWithLifecycle()
    val friendsState by viewModel.friendsState.collectAsStateWithLifecycle()
    val serversState by viewModel.serversState.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val claimedRewardDays by viewModel.claimedRewardDays.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val tutorialStep by viewModel.tutorialStep.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val fpsHistory by viewModel.fpsHistory.collectAsStateWithLifecycle()
    val geminiChatState by viewModel.geminiChatState.collectAsStateWithLifecycle()
    val geminiChatHistory by viewModel.geminiChatHistory.collectAsStateWithLifecycle()
    val geminiBlueprintState by viewModel.geminiBlueprintState.collectAsStateWithLifecycle()
    val useEmbeddedWebHtml = true
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebPageLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(isWebPageLoaded, profile) {
        if (isWebPageLoaded) {
            val safeName = JSONObject.quote(profile.username)
            webViewRef?.evaluateJavascript(
                "window.syncNativeProfile && window.syncNativeProfile($safeName, ${profile.level}, ${profile.xp}, ${profile.coins}, ${profile.gems});",
                null
            )
        }
    }

    LaunchedEffect(isWebPageLoaded, geminiChatState) {
        if (!isWebPageLoaded) return@LaunchedEffect
        when (val st = geminiChatState) {
            GeminiChatUiState.Loading -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiChatUpdate && window.onNativeGeminiChatUpdate('loading', '');",
                null
            )
            is GeminiChatUiState.Streaming -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiChatUpdate && window.onNativeGeminiChatUpdate('streaming', ${JSONObject.quote(st.text)});",
                null
            )
            is GeminiChatUiState.Success -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiChatUpdate && window.onNativeGeminiChatUpdate('success', ${JSONObject.quote(st.text)});",
                null
            )
            is GeminiChatUiState.Error -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiChatUpdate && window.onNativeGeminiChatUpdate('error', ${JSONObject.quote(st.message)});",
                null
            )
            GeminiChatUiState.Idle -> Unit
        }
    }

    LaunchedEffect(isWebPageLoaded, geminiBlueprintState) {
        if (!isWebPageLoaded) return@LaunchedEffect
        when (val bp = geminiBlueprintState) {
            GeminiBlueprintUiState.Loading -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiBlueprint && window.onNativeGeminiBlueprint('loading', null);",
                null
            )
            is GeminiBlueprintUiState.Success -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiBlueprint && window.onNativeGeminiBlueprint('success', ${bp.template.toWebJson()});",
                null
            )
            is GeminiBlueprintUiState.Error -> webViewRef?.evaluateJavascript(
                "window.onNativeGeminiBlueprint && window.onNativeGeminiBlueprint('error', ${JSONObject.quote(bp.message)});",
                null
            )
            GeminiBlueprintUiState.Idle -> Unit
        }
    }

    BackHandler(enabled = activeModal != ActiveModal.NONE || webViewRef?.canGoBack() == true) {
        if (activeModal != ActiveModal.NONE && (activeModal == ActiveModal.TITLE_SCREEN || useEmbeddedWebHtml)) {
            viewModel.closeModal()
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.openModal(ActiveModal.TITLE_SCREEN)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (useEmbeddedWebHtml) {
            val hostContext = LocalContext.current
            DisposableEffect(Unit) {
                onDispose {
                    webViewRef?.stopLoading()
                    webViewRef?.destroy()
                    webViewRef = null
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("blockmine_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        addJavascriptInterface(BlockMineWebBridge(hostContext, viewModel, onSignOut), "BlockMineNative")
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebPageLoaded = true
                            }
                        }
                        loadUrl("file:///android_asset/index.html")
                        webViewRef = this
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 3D Voxel World Fullscreen Canvas
                VoxelWorldCanvas(
                    worldBlocks = worldBlocks,
                    playerX = playerX,
                    playerY = playerY,
                    playerZ = playerZ,
                    playerYaw = playerYaw,
                    playerPitch = playerPitch,
                    targetedRaycast = targetedRaycast,
                    onlinePeers = onlinePeers,
                    graphicsConfig = graphicsConfig,
                    onRotateCamera = { dyaw, dpitch -> viewModel.rotateCamera(dyaw, dpitch) },
                    onCanvasTapAction = { viewModel.placeSelectedBlock() }
                )

        // 2. Center Crosshair & Targeted Block Indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(Color.White)
                    )
                }
                targetedRaycast?.let { hit ->
                    val targetBlock = BlockCatalog.byId(hit.blockId)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎯 ${targetBlock.localizedName(lang)} (${hit.hitCoord.x}, ${hit.hitCoord.y}, ${hit.hitCoord.z})",
                        color = Color(0xFFFFF254),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0x99000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 3. Top HUD Bar + Quick Action Strip + Telemetry
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .alpha(graphicsConfig.hudOpacity),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudPillButton(
                    text = "🏠 ${strings.homeMenu}",
                    bgColor = Color(0xFFB45309),
                    tag = "btn_hud_title",
                    onClick = { viewModel.openModal(ActiveModal.TITLE_SCREEN) }
                )
                HudPillButton(
                    text = "🎒 Envanter",
                    bgColor = Color(0xFF047857),
                    tag = "btn_hud_inv",
                    onClick = { viewModel.openModal(ActiveModal.INVENTORY) }
                )
                HudPillButton(
                    text = "🏰 Şablonlar",
                    bgColor = Color(0xFFD97706),
                    tag = "btn_hud_templates",
                    onClick = { viewModel.openModal(ActiveModal.TEMPLATES) }
                )
                HudPillButton(
                    text = "🌐 Sunucu",
                    bgColor = Color(0xFF1D4ED8),
                    tag = "btn_hud_servers",
                    onClick = { viewModel.openModal(ActiveModal.MULTIPLAYER_SERVERS) }
                )
                HudPillButton(
                    text = "👥 Arkadaşlar",
                    bgColor = Color(0xFF0E7490),
                    tag = "btn_hud_friends",
                    onClick = { viewModel.openModal(ActiveModal.FRIENDS_LIST) }
                )
                HudPillButton(
                    text = "🎁 Görev & Ödül",
                    bgColor = Color(0xFFBE185D),
                    tag = "btn_hud_quests",
                    onClick = { viewModel.openModal(ActiveModal.DAILY_QUESTS_REWARDS) }
                )
                HudPillButton(
                    text = "🏆 Liderlik",
                    bgColor = Color(0xFFA16207),
                    tag = "btn_hud_leaderboard",
                    onClick = { viewModel.openModal(ActiveModal.LEADERBOARD) }
                )
                HudPillButton(
                    text = "💬 ${strings.chatAction}",
                    bgColor = Color(0xFF4338CA),
                    tag = "btn_hud_chat",
                    onClick = { viewModel.openModal(ActiveModal.CHAT_DRAWER) }
                )
                HudPillButton(
                    text = "⚙️ Ayarlar",
                    bgColor = Color(0xFF334155),
                    tag = "btn_hud_settings",
                    onClick = { viewModel.openModal(ActiveModal.GRAPHICS_SETTINGS) }
                )
                HudPillButton(
                    text = "✨ Gemini AI",
                    bgColor = Color(0xFF7C3AED),
                    tag = "btn_hud_gemini_ai",
                    onClick = { viewModel.openModal(ActiveModal.GEMINI_AI) }
                )
            }

            // Telemetry & Player Stats Bar
            Row(
                modifier = Modifier
                    .background(Color(0xBF090D16), RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFF5EBD3E), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BLOCKMİNE",
                    color = Color(0xFF5EBD3E),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "60 FPS | XYZ: %.1f / %.1f / %.1f",
                        playerX,
                        playerY,
                        playerZ
                    ),
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("hud_xyz_text")
                )
                Text(
                    text = "⭐ Sv.${profile.level} | 🪙 ${profile.coins} | 💎 ${profile.gems}",
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Animated Feedback Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xE6064E3B), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFFFF254), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("status_feedback_banner")
                ) {
                    Text(
                        text = statusBanner.orEmpty(),
                        color = Color(0xFFFFF254),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 4. Bottom HUD: Virtual Joystick, Action Pad (Kır / Koy / Zıpla / Şablon), and 8-Slot Hotbar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .alpha(graphicsConfig.hudOpacity),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Movement Joystick + Quick D-Pad + Jump Button
                VirtualMovementControlCluster(
                    jumpLabel = strings.jumpAction,
                    onMove = { forward, strafe -> viewModel.movePlayer(forward, strafe) },
                    onJump = { viewModel.jumpPlayer() }
                )

                // Right Action Pad: Break (Kır), Place (Koy), Quick Template Build
                Column(
                    modifier = Modifier
                        .background(Color(0xBF090D16), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.breakTargetedBlock() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pad_attack")
                        ) {
                            Text("⛏️ ${strings.breakBlock}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.placeSelectedBlock() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pad_place")
                        ) {
                            Text("🧱 ${strings.placeBlock}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.buildStructureTemplate() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("pad_build_template")
                    ) {
                        Text(
                            text = "🏰 ${strings.spawnTemplate}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Active Block Banner + 8-Slot Hotbar
            val activeBlockId = hotbarBlockIds.getOrElse(selectedSlot) { 1 }
            val activeBlock = BlockCatalog.byId(activeBlockId)
            Box(
                modifier = Modifier
                    .background(Color(0xCC090D16), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🧱 ${activeBlock.localizedName(lang)} (Slot #${selectedSlot + 1})",
                    color = Color(0xFFFFF254),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("active_item_banner")
                )
            }

            Row(
                modifier = Modifier
                    .background(Color(0xE6090D16), RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(6.dp)
                    .testTag("hotbar_slots"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hotbarBlockIds.forEachIndexed { idx, blockId ->
                    val block = BlockCatalog.byId(blockId)
                    val isSelected = idx == selectedSlot
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF334155) else Color(0xFF1E293B))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFFF254) else Color(0xFF64748B),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectHotbarSlot(idx) }
                            .testTag("hotbar_slot_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(block.topColor), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(block.sideColor), RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "${idx + 1}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        // 5. Active Modal Overlay
        when (activeModal) {
            ActiveModal.NONE -> Unit
            ActiveModal.TITLE_SCREEN -> {
                TitleScreenModal(
                    strings = strings,
                    profile = profile,
                    activeServerName = activeServerName,
                    currentLanguage = lang,
                    unreadNotificationsCount = notifications.count { it.isUnread },
                    onStartGame = { viewModel.closeModal() },
                    onOpenModal = { viewModel.openModal(it) },
                    onChangeLanguage = { viewModel.setLanguage(it) },
                    onSignOut = onSignOut
                )
            }
            ActiveModal.INVENTORY -> {
                InventoryModal(
                    strings = strings,
                    lang = lang,
                    hotbarBlockIds = hotbarBlockIds,
                    selectedSlot = selectedSlot,
                    draggedBlockId = draggedBlockId,
                    onSelectSlot = { viewModel.selectHotbarSlot(it) },
                    onAssignBlockToSlot = { slot, bid -> viewModel.assignBlockToHotbar(slot, bid) },
                    onStartDragBlock = { viewModel.startDraggingBlock(it) },
                    onDropOnSlot = { viewModel.dropDraggedBlockOnHotbar(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.TEMPLATES -> {
                StructureTemplatesModal(
                    strings = strings,
                    lang = lang,
                    templates = BlockCatalog.templates,
                    onBuildTemplate = { viewModel.buildStructureTemplate(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.MULTIPLAYER_SERVERS -> {
                MultiplayerServersModal(
                    activeServerName = activeServerName,
                    serversState = serversState,
                    onJoinPresetServer = { viewModel.joinPresetServer(it) },
                    onCreateServer = { name, mode, region -> viewModel.createAndJoinServer(name, mode, region) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.FRIENDS_LIST -> {
                FriendsListModal(
                    friendsState = friendsState,
                    onlinePeers = onlinePeers,
                    onAddFriend = { name, tag -> viewModel.addFriendToCloud(name, tag) },
                    onRemoveFriend = { viewModel.removeFriendFromCloud(it) },
                    onJoinFriendServer = { viewModel.joinPresetServer(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.DAILY_QUESTS_REWARDS -> {
                DailyQuestsAndRewardsModal(
                    strings = strings,
                    lang = lang,
                    quests = dailyQuests,
                    rewardsSchedule = viewModel.dailyRewardsSchedule,
                    claimedDays = claimedRewardDays,
                    onClaimQuest = { viewModel.claimDailyQuest(it) },
                    onClaimDailyReward = { viewModel.claimDailyLoginReward(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.LEADERBOARD -> {
                GlobalLeaderboardModal(
                    strings = strings,
                    entries = viewModel.getGlobalLeaderboard(),
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.CHARACTER_CUSTOMIZER -> {
                CharacterCustomizerModal(
                    profile = profile,
                    onSave = { name, badge, skin, hair, tunic, pants, cape ->
                        viewModel.updateCharacterCustomization(name, badge, skin, hair, tunic, pants, cape)
                    },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.ANALYTICS_DASHBOARD -> {
                AnalyticsDashboardModal(
                    profile = profile,
                    fpsHistory = fpsHistory,
                    totalWorldBlocks = worldBlocks.size,
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.GRAPHICS_SETTINGS -> {
                GraphicsAndUiSettingsModal(
                    config = graphicsConfig,
                    onUpdateConfig = { viewModel.updateGraphicsConfig(it) },
                    onToggleMusic = { viewModel.toggleBackgroundMusic(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.NOTIFICATIONS_PANEL -> {
                NotificationsReportModal(
                    lang = lang,
                    reports = notifications,
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.INTERACTIVE_TUTORIAL -> {
                InteractiveTutorialModal(
                    step = tutorialStep,
                    onNextStep = { viewModel.nextTutorialStep() },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.CHAT_DRAWER -> {
                MultiplayerChatModal(
                    activeServerName = activeServerName,
                    messages = chatMessages,
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onClose = { viewModel.closeModal() }
                )
            }
            ActiveModal.GEMINI_AI -> Unit // Rendered in top-level Box so it works over both WebView and 3B Canvas
        }
            }
        }
    }

        if (activeModal == ActiveModal.GEMINI_AI) {
            GeminiAiArchitectModal(
                language = lang,
                chatState = geminiChatState,
                chatHistory = geminiChatHistory,
                blueprintState = geminiBlueprintState,
                onAskAssistant = { viewModel.askGeminiAssistant(it) },
                onGenerateBlueprint = { viewModel.generateAiBlueprint(it) },
                onBuildBlueprintInWorld = { template ->
                    viewModel.buildGeneratedAiBlueprint(template)
                    if (useEmbeddedWebHtml) {
                        webViewRef?.evaluateJavascript(
                            "window.spawnNativeBlueprintPlacements && window.spawnNativeBlueprintPlacements(${template.toWebJson()});",
                            null
                        )
                    }
                },
                onClose = { viewModel.closeModal() }
            )
        }
    }
}

@Composable
private fun HudPillButton(
    text: String,
    bgColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag(tag)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VirtualMovementControlCluster(
    jumpLabel: String,
    onMove: (Float, Float) -> Unit,
    onJump: () -> Unit
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Touch Joystick Circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0x99090D16))
                .border(2.dp, Color(0x88FFFFFF), CircleShape)
                .testTag("movement_joystick_zone")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            knobX = 0f
                            knobY = 0f
                        },
                        onDragCancel = {
                            knobX = 0f
                            knobY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nx = knobX + dragAmount.x
                            val ny = knobY + dragAmount.y
                            val dist = hypot(nx, ny).coerceAtMost(32f)
                            val angle = atan2(ny, nx)
                            knobX = cos(angle) * dist
                            knobY = sin(angle) * dist
                            val forward = -(knobY / 32f)
                            val strafe = (knobX / 32f)
                            onMove(forward, strafe)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                    .size(40.dp)
                    .background(Color(0xCC5EBD3E), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Quick Directional Step & Jump Row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallStepButton("⬅️", "btn_step_left") { onMove(0f, -1f) }
            SmallStepButton("⬆️", "btn_step_forward") { onMove(1f, 0f) }
            SmallStepButton("⬇️", "btn_step_back") { onMove(-1f, 0f) }
            SmallStepButton("➡️", "btn_step_right") { onMove(0f, 1f) }
            Button(
                onClick = onJump,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("pad_jump")
            ) {
                Text("⏫ $jumpLabel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SmallStepButton(
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC1E293B))
            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}
