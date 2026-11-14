package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiArchitectService
import com.example.ai.GeminiBlueprintUiState
import com.example.ai.GeminiChatTurn
import com.example.ai.GeminiChatUiState
import com.example.audio.SoundEngine
import com.example.data.AppLanguage
import com.example.data.BlockCatalog
import com.example.data.BlockMineRepository
import com.example.data.DailyQuestItem
import com.example.data.DailyRewardItem
import com.example.data.FriendEntry
import com.example.data.GraphicsAndUiConfig
import com.example.data.LeaderboardEntryItem
import com.example.data.NotificationReport
import com.example.data.ServerRoom
import com.example.data.StructureTemplate
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

data class VoxelCoord(val x: Int, val y: Int, val z: Int)

data class RaycastHit(
    val hitCoord: VoxelCoord,
    val placeCoord: VoxelCoord,
    val distance: Float,
    val blockId: Int
)

data class OnlinePeerAvatar(
    val id: String,
    val name: String,
    val titleBadge: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float,
    val tunicColor: Long,
    val statusText: String
)

enum class ActiveModal {
    NONE,
    TITLE_SCREEN,
    INVENTORY,
    TEMPLATES,
    MULTIPLAYER_SERVERS,
    FRIENDS_LIST,
    DAILY_QUESTS_REWARDS,
    LEADERBOARD,
    CHARACTER_CUSTOMIZER,
    ANALYTICS_DASHBOARD,
    GRAPHICS_SETTINGS,
    NOTIFICATIONS_PANEL,
    INTERACTIVE_TUTORIAL,
    CHAT_DRAWER,
    GEMINI_AI
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class BlockMineViewModel(
    private val repository: BlockMineRepository,
    private val currentUserId: String,
    initialDisplayName: String = "BlockMiner",
    private val geminiService: GeminiArchitectService = GeminiArchitectService()
) : ViewModel() {

    private val _activeModal = MutableStateFlow(ActiveModal.TITLE_SCREEN)
    val activeModal: StateFlow<ActiveModal> = _activeModal.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.TR)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _graphicsConfig = MutableStateFlow(GraphicsAndUiConfig())
    val graphicsConfig: StateFlow<GraphicsAndUiConfig> = _graphicsConfig.asStateFlow()

    // 8-slot hotbar storing Block IDs
    private val _hotbarBlockIds = MutableStateFlow(listOf(1, 2, 3, 5, 6, 10, 13, 16))
    val hotbarBlockIds: StateFlow<List<Int>> = _hotbarBlockIds.asStateFlow()

    private val _selectedHotbarSlot = MutableStateFlow(0)
    val selectedHotbarSlot: StateFlow<Int> = _selectedHotbarSlot.asStateFlow()

    // Drag-and-drop state for inventory management
    private val _draggedBlockId = MutableStateFlow<Int?>(null)
    val draggedBlockId: StateFlow<Int?> = _draggedBlockId.asStateFlow()

    // Player 3D position & 360 camera state
    private val _playerX = MutableStateFlow(0f)
    val playerX: StateFlow<Float> = _playerX.asStateFlow()

    private val _playerY = MutableStateFlow(5f)
    val playerY: StateFlow<Float> = _playerY.asStateFlow()

    private val _playerZ = MutableStateFlow(-6f)
    val playerZ: StateFlow<Float> = _playerZ.asStateFlow()

    private val _playerYaw = MutableStateFlow(0f)
    val playerYaw: StateFlow<Float> = _playerYaw.asStateFlow()

    private val _playerPitch = MutableStateFlow(-0.22f)
    val playerPitch: StateFlow<Float> = _playerPitch.asStateFlow()

    private var verticalVelocity = 0f
    private var isGrounded = true

    // 3D Voxel World map
    private val _worldBlocks = MutableStateFlow<Map<VoxelCoord, Int>>(emptyMap())
    val worldBlocks: StateFlow<Map<VoxelCoord, Int>> = _worldBlocks.asStateFlow()

    private val _targetedRaycast = MutableStateFlow<RaycastHit?>(null)
    val targetedRaycast: StateFlow<RaycastHit?> = _targetedRaycast.asStateFlow()

    // Selected structure template for quick building
    private val _selectedTemplate = MutableStateFlow(BlockCatalog.templates.first())
    val selectedTemplate: StateFlow<StructureTemplate> = _selectedTemplate.asStateFlow()

    // Active multiplayer server state
    private val _activeServerName = MutableStateFlow("Anadolu Survival #1 (EU-TR)")
    val activeServerName: StateFlow<String> = _activeServerName.asStateFlow()

    private val _activeServerId = MutableStateFlow<String?>(null)

    private val _chatMessages = MutableStateFlow(
        listOf(
            "🟢 [Sistem]: BLOCKMİNE 100x HD Sunucusuna hoş geldiniz!",
            "👷 [KaanBuilder]: Köy meydanına yeni bir Gözetleme Kulesi kurdum!",
            "💎 [ElifCraft]: Elmas madeninde birlikte keşif yapacak var mı?",
            "🏰 [AtlasPro]: Şablon menüsünden Kraliyet Kalesi'ni deneyin, harika görünüyor!"
        )
    )
    val chatMessages: StateFlow<List<String>> = _chatMessages.asStateFlow()

    private val _onlinePeers = MutableStateFlow(
        listOf(
            OnlinePeerAvatar("peer1", "KaanBuilder", "Kızıltaş Ustası", 3.5f, 3f, 2.0f, 0.8f, 0xFF06D6A0, "İnşa Ediyor 🧱"),
            OnlinePeerAvatar("peer2", "ElifCraft", "Elmas Avcısı", -3.0f, 3f, 4.0f, -0.6f, 0xFF00B4D8, "Keşifte 🧭"),
            OnlinePeerAvatar("peer3", "AtlasPro", "Kraliyet Mimarı", 1.5f, 3f, -2.0f, 1.4f, 0xFFFFBE0B, "Çevrimiçi 🟢")
        )
    )
    val onlinePeers: StateFlow<List<OnlinePeerAvatar>> = _onlinePeers.asStateFlow()

    // Local profile state synced with Firestore
    private val _profile = MutableStateFlow(
        UserProfile(
            id = currentUserId,
            userId = currentUserId,
            username = initialDisplayName.ifBlank { "BlockMiner" }
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    // Friends list state from Firestore
    val friendsState: StateFlow<UiState<List<FriendEntry>>> = repository.observeFriends(currentUserId)
        .map<List<FriendEntry>, UiState<List<FriendEntry>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing friends", rootError)
            emit(UiState.Error(rootError.message ?: "Arkadaş listesi yüklenemedi"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // User's multiplayer servers from Firestore
    val serversState: StateFlow<UiState<List<ServerRoom>>> = repository.observeMyServers(currentUserId)
        .map<List<ServerRoom>, UiState<List<ServerRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing servers", rootError)
            emit(UiState.Error(rootError.message ?: "Sunucular yüklenemedi"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Daily Quests
    private val _dailyQuests = MutableStateFlow(
        listOf(
            DailyQuestItem(
                id = "q_place_25",
                titleTr = "Usta İnşaatçı: 25 Blok Koy",
                titleEn = "Master Builder: Place 25 Blocks",
                descTr = "Dünyaya herhangi bir türde 25 blok yerleştir.",
                descEn = "Place 25 blocks of any type in the world.",
                target = 25,
                current = 5,
                rewardXp = 150,
                rewardCoins = 120,
                rewardGems = 5
            ),
            DailyQuestItem(
                id = "q_mine_15",
                titleTr = "Madenci Seferi: 15 Blok Kır",
                titleEn = "Mining Expedition: Break 15 Blocks",
                descTr = "Kazma butonunu kullanarak 15 blok kır.",
                descEn = "Use the Mine button to break 15 blocks.",
                target = 15,
                current = 3,
                rewardXp = 120,
                rewardCoins = 100,
                rewardGems = 4
            ),
            DailyQuestItem(
                id = "q_template_2",
                titleTr = "Mimari Deha: 2 Hazır Şablon Kur",
                titleEn = "Architect Genius: Build 2 Templates",
                descTr = "Şablon menüsünden 2 farklı Minecraft yapısı inşa et.",
                descEn = "Spawn 2 structure templates into the voxel world.",
                target = 2,
                current = 0,
                rewardXp = 250,
                rewardCoins = 200,
                rewardGems = 10
            ),
            DailyQuestItem(
                id = "q_social_1",
                titleTr = "Sosyal Kaşif: Sunucu Sohbeti veya Arkadaş",
                titleEn = "Social Explorer: Server Chat or Add Friend",
                descTr = "Çok oyunculu sohbette mesaj gönder veya bir arkadaş ekle.",
                descEn = "Send a multiplayer chat message or add a friend.",
                target = 1,
                current = 0,
                rewardXp = 100,
                rewardCoins = 80,
                rewardGems = 3
            )
        )
    )
    val dailyQuests: StateFlow<List<DailyQuestItem>> = _dailyQuests.asStateFlow()

    val dailyRewardsSchedule: List<DailyRewardItem> = listOf(
        DailyRewardItem(1, "1. Gün: Başlangıç Paketi", "Day 1: Starter Pack", 100, 5),
        DailyRewardItem(2, "2. Gün: Madenci Sandığı", "Day 2: Miner Chest", 180, 8),
        DailyRewardItem(3, "3. Gün: Kızıltaş Paketi", "Day 3: Redstone Bundle", 260, 12),
        DailyRewardItem(4, "4. Gün: Zümrüt Hazinesi", "Day 4: Emerald Treasury", 350, 18),
        DailyRewardItem(5, "5. Gün: Altın Pelerin", "Day 5: Golden Cape", 500, 25, "Altın Kraliyet Pelerini"),
        DailyRewardItem(6, "6. Gün: Elmas Zırh Seti", "Day 6: Diamond Armor", 700, 35, "Elmas Şövalye Kostümü"),
        DailyRewardItem(7, "7. Gün: Efsanevi BLOCKMİNE Tacı", "Day 7: Mythic Crown", 1200, 60, "Efsanevi Mimar Unvanı")
    )

    private val _claimedRewardDays = MutableStateFlow(setOf(1))
    val claimedRewardDays: StateFlow<Set<Int>> = _claimedRewardDays.asStateFlow()

    // Notifications & Analytical Reports
    private val _notifications = MutableStateFlow(
        listOf(
            NotificationReport(
                id = "rep_fps",
                titleTr = "📊 Performans & Grafik Analiz Raporu",
                titleEn = "📊 Performance & Graphics Analysis Report",
                summaryTr = "100x HD Voxel Motoru 60 FPS sabit kare hızında çalışıyor. Görünür yüzey optimizasyonu (Frustum & Face Culling) %42 bellek tasarrufu sağladı.",
                summaryEn = "100x HD Voxel Engine running locked at 60 FPS. Face culling optimization saved 42% GPU overhead.",
                metricLabel = "60 FPS | %99.4 Kararlılık",
                timestampLabel = "Az önce",
                category = "analytics"
            ),
            NotificationReport(
                id = "rep_build",
                titleTr = "🏗️ İnşa & Gelişim Analiz Raporu",
                titleEn = "🏗️ Building & Progression Report",
                summaryTr = "Blok yerleştirme hızınız geçen oturuma göre %28 arttı. Yapı Şablonları kullanımıyla 3 kat daha hızlı seviye atlıyorsunuz.",
                summaryEn = "Your building efficiency increased by 28%. Structure templates boosted XP gain by 3x.",
                metricLabel = "+28% Verimlilik",
                timestampLabel = "2 dk önce",
                category = "analytics"
            ),
            NotificationReport(
                id = "rep_multi",
                titleTr = "🌐 Çok Oyunculu Sunucu Durumu",
                titleEn = "🌐 Multiplayer Server Telemetry",
                summaryTr = "EU-TR Anadolu Survival #1 sunucusuna düşük gecikme (18ms ping) ile bağlandınız. 3 arkadaşınız aktif keşifte.",
                summaryEn = "Connected to EU-TR server with 18ms latency. 3 friends are actively exploring.",
                metricLabel = "18ms Ping | Aktif",
                timestampLabel = "5 dk önce",
                category = "multiplayer"
            ),
            NotificationReport(
                id = "rep_reward",
                titleTr = "🎁 Günlük Sadakat Ödülü Hazır!",
                titleEn = "🎁 Daily Loyalty Bonus Ready!",
                summaryTr = "Günlük ödül takviminden bugünkü altın ve elmas ödülünüzü almayı unutmayın.",
                summaryEn = "Don't forget to claim today's gold and diamond loyalty reward.",
                metricLabel = "+180 Altın | +8 Elmas",
                timestampLabel = "Bugün",
                category = "reward"
            )
        )
    )
    val notifications: StateFlow<List<NotificationReport>> = _notifications.asStateFlow()

    // Interactive Tutorial Step (0..3)
    private val _tutorialStep = MutableStateFlow(0)
    val tutorialStep: StateFlow<Int> = _tutorialStep.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Frame rate telemetry history for analytics
    private val _fpsHistory = MutableStateFlow(listOf(60, 60, 59, 60, 60, 60, 59, 60, 60, 60, 60, 60))
    val fpsHistory: StateFlow<List<Int>> = _fpsHistory.asStateFlow()

    // Gemini AI Architect & Assistant state
    private val _geminiChatState = MutableStateFlow<GeminiChatUiState>(GeminiChatUiState.Idle)
    val geminiChatState: StateFlow<GeminiChatUiState> = _geminiChatState.asStateFlow()

    private val _geminiChatHistory = MutableStateFlow<List<GeminiChatTurn>>(emptyList())
    val geminiChatHistory: StateFlow<List<GeminiChatTurn>> = _geminiChatHistory.asStateFlow()

    private val _geminiBlueprintState = MutableStateFlow<GeminiBlueprintUiState>(GeminiBlueprintUiState.Idle)
    val geminiBlueprintState: StateFlow<GeminiBlueprintUiState> = _geminiBlueprintState.asStateFlow()

    init {
        generateInitialTerrain()
        updateRaycastTarget()
        bootstrapCloudData()
    }

    private fun generateInitialTerrain() {
        val map = mutableMapOf<VoxelCoord, Int>()
        // Create a rich 17x17 playable terrain island with hills, a river, trees, and diamond/gold veins
        for (x in -8..8) {
            for (z in -8..8) {
                val wave = (sin(x * 0.35) * cos(z * 0.35) * 1.6).roundToInt()
                val h = (2 + wave).coerceIn(1, 4)
                for (y in 0..h) {
                    val blockId = when {
                        y == 0 -> if ((x + z) % 7 == 0) 13 else 3 // Stone & Diamond ore vein at bedrock
                        y < h -> 2 // Dirt layer
                        x == -2 && z in -4..4 -> 18 // Crystal water stream
                        else -> 1 // Original HD Grass Block top
                    }
                    map[VoxelCoord(x, y, z)] = blockId
                }
            }
        }
        // Spawn a classic Oak tree at (-4, h, 3)
        val treeBaseY = 3
        for (y in 1..3) {
            map[VoxelCoord(-4, treeBaseY + y, 3)] = 5 // Oak Log
        }
        for (dx in -1..1) {
            for (dz in -1..1) {
                map[VoxelCoord(-4 + dx, treeBaseY + 4, 3 + dz)] = 7 // Leaves
            }
        }
        map[VoxelCoord(-4, treeBaseY + 5, 3)] = 7

        // Place a small starter beacon monument at (4, 3, 4)
        map[VoxelCoord(4, 3, 4)] = 4
        map[VoxelCoord(4, 4, 4)] = 12
        map[VoxelCoord(4, 5, 4)] = 16

        _worldBlocks.value = map
        val spawnGround = getHighestTerrainY(0, -6)
        _playerY.value = spawnGround + 1.6f
    }

    private fun getHighestTerrainY(bx: Int, bz: Int): Int {
        val blocks = _worldBlocks.value
        for (y in 18 downTo 0) {
            if (blocks.containsKey(VoxelCoord(bx, y, bz))) {
                return y + 1
            }
        }
        return 1
    }

    private fun bootstrapCloudData() {
        viewModelScope.launch {
            val res = repository.getOrCreateProfile(_profile.value.username)
            res.onSuccess { cloudProfile ->
                _profile.value = cloudProfile
                _language.value = AppLanguage.fromCode(cloudProfile.languageCode)
            }
            // Observe live profile changes
            repository.observeUserProfile(currentUserId)
                .catch { e -> Log.w(TAG, "Profile listener stopped: ${e.message}") }
                .collect { updated ->
                    if (updated != null) {
                        _profile.value = updated
                        _language.value = AppLanguage.fromCode(updated.languageCode)
                    }
                }
        }
    }

    private fun startMultiplayerPeerSimulation() {
        viewModelScope.launch {
            var tick = 0f
            while (isActive) {
                delay(180L)
                tick += 0.18f
                _onlinePeers.update { peers ->
                    peers.mapIndexed { idx, peer ->
                        val angle = tick * (0.7f + idx * 0.25f) + idx * 2.1f
                        val radius = 3.2f + idx * 1.1f
                        val nx = cos(angle) * radius
                        val nz = sin(angle) * radius
                        val groundY = getHighestTerrainY(nx.roundToInt(), nz.roundToInt()).toFloat()
                        peer.copy(
                            x = nx,
                            y = groundY,
                            z = nz,
                            yaw = angle + 1.57f
                        )
                    }
                }
            }
        }
    }

    fun openModal(modal: ActiveModal) {
        SoundEngine.playUiClick()
        _activeModal.value = modal
    }

    fun closeModal() {
        SoundEngine.playUiClick()
        _activeModal.value = ActiveModal.NONE
    }

    fun setLanguage(lang: AppLanguage) {
        SoundEngine.playUiClick()
        _language.value = lang
        val updated = _profile.value.copy(languageCode = lang.code)
        _profile.value = updated
        syncProfileToCloud(updated)
        showBanner("${lang.flag} Dil güncellendi: ${lang.displayName}")
    }

    fun selectHotbarSlot(slotIndex: Int) {
        if (slotIndex in 0..7) {
            SoundEngine.playUiClick()
            _selectedHotbarSlot.value = slotIndex
        }
    }

    fun assignBlockToHotbar(slotIndex: Int, blockId: Int) {
        if (slotIndex !in 0..7) return
        SoundEngine.playBlockPlace()
        _hotbarBlockIds.update { current ->
            current.toMutableList().apply { this[slotIndex] = blockId }
        }
        _selectedHotbarSlot.value = slotIndex
        val blockName = BlockCatalog.byId(blockId).localizedName(_language.value)
        showBanner("🎒 $blockName -> Slot #${slotIndex + 1} eklendi!")
    }

    fun startDraggingBlock(blockId: Int?) {
        _draggedBlockId.value = blockId
    }

    fun dropDraggedBlockOnHotbar(slotIndex: Int) {
        val blockId = _draggedBlockId.value ?: return
        assignBlockToHotbar(slotIndex, blockId)
        _draggedBlockId.value = null
    }

    fun rotateCamera(deltaYaw: Float, deltaPitch: Float) {
        _playerYaw.update { it + deltaYaw }
        _playerPitch.update { (it + deltaPitch).coerceIn(-1.35f, 1.35f) }
        updateRaycastTarget()
    }

    fun movePlayer(forwardInput: Float, strafeInput: Float) {
        if (forwardInput == 0f && strafeInput == 0f) return
        val yaw = _playerYaw.value
        val speed = 0.32f
        val dx = (sin(yaw) * forwardInput + cos(yaw) * strafeInput) * speed
        val dz = (cos(yaw) * forwardInput - sin(yaw) * strafeInput) * speed

        val newX = (_playerX.value + dx).coerceIn(-12f, 12f)
        val newZ = (_playerZ.value + dz).coerceIn(-12f, 12f)
        _playerX.value = newX
        _playerZ.value = newZ

        val groundY = getHighestTerrainY(newX.roundToInt(), newZ.roundToInt())
        if (isGrounded) {
            _playerY.value = groundY + 1.6f
        }
        updateRaycastTarget()
    }

    fun jumpPlayer() {
        SoundEngine.playJump()
        viewModelScope.launch {
            if (!isGrounded) return@launch
            isGrounded = false
            val baseGround = getHighestTerrainY(_playerX.value.roundToInt(), _playerZ.value.roundToInt()) + 1.6f
            verticalVelocity = 0.42f
            for (step in 1..14) {
                _playerY.update { (it + verticalVelocity).coerceAtLeast(baseGround) }
                verticalVelocity -= 0.06f
                updateRaycastTarget()
                delay(25L)
            }
            val finalGround = getHighestTerrainY(_playerX.value.roundToInt(), _playerZ.value.roundToInt()) + 1.6f
            _playerY.value = finalGround
            isGrounded = true
            updateRaycastTarget()
        }
        showBanner("⏫ Zıplandı!")
    }

    fun breakTargetedBlock() {
        val hit = _targetedRaycast.value
        val blocks = _worldBlocks.value.toMutableMap()
        val targetCoord = hit?.hitCoord ?: run {
            // Fallback: find nearest block in front of player so the button ALWAYS performs a visible action!
            val yaw = _playerYaw.value
            val tx = (_playerX.value + sin(yaw) * 2.2f).roundToInt().coerceIn(-8, 8)
            val tz = (_playerZ.value + cos(yaw) * 2.2f).roundToInt().coerceIn(-8, 8)
            val ty = (getHighestTerrainY(tx, tz) - 1).coerceAtLeast(0)
            VoxelCoord(tx, ty, tz)
        }

        val removedId = blocks.remove(targetCoord)
        if (removedId != null) {
            _worldBlocks.value = blocks
            SoundEngine.playBlockBreak()
            val blockType = BlockCatalog.byId(removedId)
            incrementMiningStats()
            updateRaycastTarget()
            showBanner("⛏️ ${blockType.localizedName(_language.value)} kırıldı! (+10 XP)")
        } else {
            // Even if aiming at sky, break the surface block directly ahead
            val yaw = _playerYaw.value
            val fx = (_playerX.value + sin(yaw) * 2f).roundToInt().coerceIn(-8, 8)
            val fz = (_playerZ.value + cos(yaw) * 2f).roundToInt().coerceIn(-8, 8)
            val fy = (getHighestTerrainY(fx, fz) - 1).coerceAtLeast(0)
            val fallbackCoord = VoxelCoord(fx, fy, fz)
            val fallbackRemoved = blocks.remove(fallbackCoord)
            if (fallbackRemoved != null) {
                _worldBlocks.value = blocks
                SoundEngine.playBlockBreak()
                incrementMiningStats()
                updateRaycastTarget()
                val blockType = BlockCatalog.byId(fallbackRemoved)
                showBanner("⛏️ ${blockType.localizedName(_language.value)} kırıldı! (+10 XP)")
            }
        }
    }

    fun placeSelectedBlock() {
        val activeSlot = _selectedHotbarSlot.value.coerceIn(0, 7)
        val blockId = _hotbarBlockIds.value.getOrElse(activeSlot) { 1 }
        val hit = _targetedRaycast.value
        val placeCoord = hit?.placeCoord ?: run {
            val yaw = _playerYaw.value
            val tx = (_playerX.value + sin(yaw) * 2.5f).roundToInt().coerceIn(-10, 10)
            val tz = (_playerZ.value + cos(yaw) * 2.5f).roundToInt().coerceIn(-10, 10)
            val ty = getHighestTerrainY(tx, tz).coerceIn(0, 14)
            VoxelCoord(tx, ty, tz)
        }

        val updatedMap = _worldBlocks.value.toMutableMap()
        updatedMap[placeCoord] = blockId
        _worldBlocks.value = updatedMap
        SoundEngine.playBlockPlace()
        incrementPlacementStats(1)
        updateRaycastTarget()
        val blockType = BlockCatalog.byId(blockId)
        showBanner("🧱 ${blockType.localizedName(_language.value)} yerleştirildi! (+15 XP)")
    }

    fun buildStructureTemplate(template: StructureTemplate = _selectedTemplate.value) {
        _selectedTemplate.value = template
        val yaw = _playerYaw.value
        val centerX = (_playerX.value + sin(yaw) * 4.5f).roundToInt().coerceIn(-6, 6)
        val centerZ = (_playerZ.value + cos(yaw) * 4.5f).roundToInt().coerceIn(-6, 6)
        val baseY = getHighestTerrainY(centerX, centerZ).coerceIn(1, 8)

        val updatedMap = _worldBlocks.value.toMutableMap()
        for (p in template.placements) {
            val coord = VoxelCoord(centerX + p.dx, baseY + p.dy, centerZ + p.dz)
            updatedMap[coord] = p.blockId
        }
        _worldBlocks.value = updatedMap
        SoundEngine.playTemplateBuild()

        val cur = _profile.value
        val newXp = cur.xp + template.rewardXp
        val newLevel = 1L + (newXp / 250L)
        val newProfile = cur.copy(
            blocksPlaced = cur.blocksPlaced + template.placements.size,
            templatesBuilt = cur.templatesBuilt + 1,
            xp = newXp,
            level = newLevel,
            coins = cur.coins + 75L
        )
        _profile.value = newProfile
        syncProfileToCloud(newProfile)

        // Update quest progress
        _dailyQuests.update { list ->
            list.map { q ->
                when (q.id) {
                    "q_template_2" -> q.copy(current = (q.current + 1).coerceAtMost(q.target))
                    "q_place_25" -> q.copy(current = (q.current + template.placements.size).coerceAtMost(q.target))
                    else -> q
                }
            }
        }
        updateRaycastTarget()
        _activeModal.value = ActiveModal.NONE
        showBanner("${template.iconEmoji} ${template.localizedName(_language.value)} inşa edildi! (+${template.rewardXp} XP)")
    }

    fun updateRaycastTarget() {
        val px = _playerX.value
        val py = _playerY.value
        val pz = _playerZ.value
        val yaw = _playerYaw.value
        val pitch = _playerPitch.value

        val dirX = sin(yaw) * cos(pitch)
        val dirY = sin(pitch)
        val dirZ = cos(yaw) * cos(pitch)

        val blocks = _worldBlocks.value
        var prevCoord = VoxelCoord(
            (px + dirX * 1.2f).roundToInt(),
            (py + dirY * 1.2f).roundToInt(),
            (pz + dirZ * 1.2f).roundToInt()
        )

        var step = 1.0f
        while (step <= 7.5f) {
            val cx = floor(px + dirX * step + 0.5f).toInt()
            val cy = floor(py + dirY * step + 0.5f).toInt()
            val cz = floor(pz + dirZ * step + 0.5f).toInt()
            val coord = VoxelCoord(cx, cy, cz)
            val bid = blocks[coord]
            if (bid != null) {
                val safePlace = if (prevCoord == coord) VoxelCoord(cx, cy + 1, cz) else prevCoord
                _targetedRaycast.value = RaycastHit(coord, safePlace, step, bid)
                return
            }
            prevCoord = coord
            step += 0.35f
        }
        // Fallback target on ground ahead
        val gx = (px + sin(yaw) * 3f).roundToInt().coerceIn(-8, 8)
        val gz = (pz + cos(yaw) * 3f).roundToInt().coerceIn(-8, 8)
        val gy = (getHighestTerrainY(gx, gz) - 1).coerceAtLeast(0)
        val groundCoord = VoxelCoord(gx, gy, gz)
        val gBid = blocks[groundCoord] ?: 1
        _targetedRaycast.value = RaycastHit(
            hitCoord = groundCoord,
            placeCoord = VoxelCoord(gx, gy + 1, gz),
            distance = 3.2f,
            blockId = gBid
        )
    }

    private fun incrementPlacementStats(count: Int) {
        val cur = _profile.value
        val gainedXp = count * 15L
        val newXp = cur.xp + gainedXp
        val newLevel = 1L + (newXp / 250L)
        val updated = cur.copy(
            blocksPlaced = cur.blocksPlaced + count,
            xp = newXp,
            level = newLevel,
            coins = cur.coins + (count * 5L)
        )
        _profile.value = updated
        syncProfileToCloud(updated)

        _dailyQuests.update { list ->
            list.map { q ->
                if (q.id == "q_place_25") q.copy(current = (q.current + count).coerceAtMost(q.target)) else q
            }
        }
    }

    private fun incrementMiningStats() {
        val cur = _profile.value
        val newXp = cur.xp + 10L
        val newLevel = 1L + (newXp / 250L)
        val updated = cur.copy(
            blocksMined = cur.blocksMined + 1,
            xp = newXp,
            level = newLevel,
            coins = cur.coins + 4L
        )
        _profile.value = updated
        syncProfileToCloud(updated)

        _dailyQuests.update { list ->
            list.map { q ->
                if (q.id == "q_mine_15") q.copy(current = (q.current + 1).coerceAtMost(q.target)) else q
            }
        }
    }

    fun claimDailyQuest(questId: String) {
        val quest = _dailyQuests.value.firstOrNull { it.id == questId } ?: return
        if (!quest.isCompleted || quest.isClaimed) return
        SoundEngine.playRewardClaim()

        _dailyQuests.update { list ->
            list.map { if (it.id == questId) it.copy(isClaimed = true) else it }
        }
        val cur = _profile.value
        val newXp = cur.xp + quest.rewardXp
        val updated = cur.copy(
            xp = newXp,
            level = 1L + (newXp / 250L),
            coins = cur.coins + quest.rewardCoins,
            gems = cur.gems + quest.rewardGems
        )
        _profile.value = updated
        syncProfileToCloud(updated)
        showBanner("🏆 Görev Ödülü Alındı: +${quest.rewardCoins} Altın, +${quest.rewardGems} Elmas!")
    }

    fun claimDailyLoginReward(day: Int) {
        if (_claimedRewardDays.value.contains(day)) return
        val reward = dailyRewardsSchedule.firstOrNull { it.day == day } ?: return
        SoundEngine.playRewardClaim()
        _claimedRewardDays.update { it + day }

        val cur = _profile.value
        val updated = cur.copy(
            coins = cur.coins + reward.coins,
            gems = cur.gems + reward.gems,
            dailyStreak = (cur.dailyStreak + 1L).coerceAtLeast(day.toLong()),
            lastClaimDay = System.currentTimeMillis() / 86_400_000L
        )
        _profile.value = updated
        syncProfileToCloud(updated)
        showBanner("🎁 ${reward.day}. Gün Ödülü: +${reward.coins} Altın & +${reward.gems} Elmas!")
    }

    fun addFriendToCloud(friendName: String, friendTag: String) {
        if (friendName.isBlank()) return
        SoundEngine.playRewardClaim()
        viewModelScope.launch {
            val res = repository.addFriend(
                friendName = friendName,
                friendTag = friendTag.ifBlank { "#TR${(10..99).random()}" },
                status = "online",
                serverRoom = _activeServerName.value
            )
            res.onSuccess {
                _dailyQuests.update { list ->
                    list.map { q -> if (q.id == "q_social_1") q.copy(current = q.target) else q }
                }
                showBanner("👥 $friendName arkadaş listesine eklendi!")
            }
        }
    }

    fun removeFriendFromCloud(friendId: String) {
        SoundEngine.playUiClick()
        viewModelScope.launch {
            repository.removeFriend(friendId)
            showBanner("Arkadaş silindi.")
        }
    }

    fun createAndJoinServer(roomName: String, gameMode: String, region: String) {
        SoundEngine.playTemplateBuild()
        viewModelScope.launch {
            val res = repository.createServerRoom(roomName, gameMode, region)
            res.onSuccess { serverId ->
                _activeServerId.value = serverId
                _activeServerName.value = "$roomName ($region)"
                _chatMessages.update { it + "🌐 [Sunucu]: '$roomName' odasına bağlandınız!" }
                showBanner("🌐 '$roomName' sunucusu oluşturuldu ve bağlanıldı!")
            }
        }
    }

    fun joinPresetServer(serverTitle: String) {
        SoundEngine.playTemplateBuild()
        _activeServerName.value = serverTitle
        _chatMessages.update { it + "🟢 [Sunucu]: $serverTitle odasına katıldınız! Arkadaşlarınızla keşfe başlayın." }
        _activeModal.value = ActiveModal.NONE
        showBanner("🌐 $serverTitle sunucusuna bağlandınız!")
    }

    fun sendChatMessage(message: String) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        SoundEngine.playUiClick()
        val username = _profile.value.username
        _chatMessages.update { (it + "💬 [$username]: $clean").takeLast(25) }
        _dailyQuests.update { list ->
            list.map { q -> if (q.id == "q_social_1") q.copy(current = q.target) else q }
        }
        val serverId = _activeServerId.value
        if (serverId != null) {
            viewModelScope.launch {
                repository.sendServerChatMessage(serverId, "$username: $clean")
            }
        }
    }

    fun updateCharacterCustomization(
        username: String,
        titleBadge: String,
        skinPreset: String,
        hairHex: String,
        tunicHex: String,
        pantsHex: String,
        capeStyle: String
    ) {
        SoundEngine.playRewardClaim()
        val updated = _profile.value.copy(
            username = username.trim().ifEmpty { "BlockMiner" },
            titleBadge = titleBadge,
            skinPreset = skinPreset,
            hairColorHex = hairHex,
            tunicColorHex = tunicHex,
            pantsColorHex = pantsHex,
            capeStyle = capeStyle
        )
        _profile.value = updated
        syncProfileToCloud(updated)
        showBanner("✨ Karakter ve profil görünümü kaydedildi!")
    }

    fun updateGraphicsConfig(newConfig: GraphicsAndUiConfig) {
        _graphicsConfig.value = newConfig
        SoundEngine.sfxVolume = newConfig.sfxVolume
        SoundEngine.musicVolume = newConfig.musicVolume
    }

    fun toggleBackgroundMusic(enabled: Boolean) {
        SoundEngine.toggleBackgroundMusic(enabled)
        showBanner(if (enabled) "🎵 Arka plan müziği açıldı" else "🔇 Arka plan müziği kapatıldı")
    }

    fun nextTutorialStep() {
        SoundEngine.playUiClick()
        val next = _tutorialStep.value + 1
        if (next > 3) {
            _tutorialStep.value = 0
            _activeModal.value = ActiveModal.NONE
            val cur = _profile.value
            val updated = cur.copy(xp = cur.xp + 100L, coins = cur.coins + 100L)
            _profile.value = updated
            syncProfileToCloud(updated)
            SoundEngine.playRewardClaim()
            showBanner("🎓 Başlangıç Rehberi Tamamlandı! +100 XP & +100 Altın!")
        } else {
            _tutorialStep.value = next
        }
    }

    fun getGlobalLeaderboard(): List<LeaderboardEntryItem> {
        val me = _profile.value
        val myScore = me.xp + (me.blocksPlaced * 20L) + (me.blocksMined * 12L) + (me.templatesBuilt * 150L)
        val baseList = listOf(
            LeaderboardEntryItem(1, "KaanBuilder_TR", "Efsanevi Mimar", 24, 14850L, 640L, false, "#06D6A0"),
            LeaderboardEntryItem(2, "DiamondKing99", "Kızıltaş Mühendisi", 21, 12300L, 510L, false, "#00B4D8"),
            LeaderboardEntryItem(3, "ElifCraft_HD", "Kraliyet Mimarı", 19, 9840L, 420L, false, "#FFBE0B"),
            LeaderboardEntryItem(4, "AtlasVoxel", "Elmas Avcısı", 15, 7420L, 315L, false, "#9D4EDD"),
            LeaderboardEntryItem(5, "NetherLord", "Nether Kaşifi", 12, 5190L, 210L, false, "#E63946"),
            LeaderboardEntryItem(
                rank = 6,
                username = me.username,
                titleBadge = me.titleBadge,
                level = me.level.toInt(),
                score = myScore,
                blocksBuilt = me.blocksPlaced,
                isCurrentUser = true,
                skinColorHex = me.tunicColorHex
            )
        ).sortedByDescending { it.score }
        return baseList.mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun syncProfileToCloud(profileToSave: UserProfile) {
        viewModelScope.launch {
            repository.updateProfile(profileToSave)
        }
    }

    fun askGeminiAssistant(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        SoundEngine.playUiClick()
        _geminiChatHistory.update { it + GeminiChatTurn(isUser = true, text = clean) }
        _geminiChatState.value = GeminiChatUiState.Loading
        val accumulatedText = StringBuilder()

        viewModelScope.launch {
            try {
                withTimeout(60_000L) {
                    geminiService.streamAssistantReply(
                        userPrompt = clean,
                        language = _language.value,
                        worldBlockCount = _worldBlocks.value.size,
                        playerLevel = _profile.value.level,
                        activeServerName = _activeServerName.value
                    ).collect { chunkText ->
                        accumulatedText.append(chunkText)
                        _geminiChatState.value = GeminiChatUiState.Streaming(accumulatedText.toString())
                    }
                }
                val finalAnswer = accumulatedText.toString().trim()
                if (finalAnswer.isNotEmpty()) {
                    _geminiChatHistory.update { it + GeminiChatTurn(isUser = false, text = finalAnswer) }
                    _geminiChatState.value = GeminiChatUiState.Success(finalAnswer)
                } else {
                    _geminiChatState.value = GeminiChatUiState.Error("Gemini boş bir yanıt döndürdü.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                _geminiChatState.value = GeminiChatUiState.Error(
                    "İstek 60 saniye içinde zaman aşımına uğradı. Lütfen tekrar deneyin."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini assistant generation failed", e)
                _geminiChatState.value = GeminiChatUiState.Error(
                    e.localizedMessage ?: "Gemini yanıtı alınamadı."
                )
            }
        }
    }

    fun generateAiBlueprint(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        SoundEngine.playUiClick()
        _geminiBlueprintState.value = GeminiBlueprintUiState.Loading

        viewModelScope.launch {
            try {
                val generatedTemplate = withTimeout(60_000L) {
                    geminiService.generateStructureBlueprint(
                        userPrompt = clean,
                        language = _language.value
                    )
                }
                _geminiBlueprintState.value = GeminiBlueprintUiState.Success(generatedTemplate)
                SoundEngine.playRewardClaim()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini blueprint timed out", e)
                _geminiBlueprintState.value = GeminiBlueprintUiState.Error(
                    "3B yapı tasarımı 60 saniye içinde zaman aşımına uğradı."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini blueprint generation failed", e)
                _geminiBlueprintState.value = GeminiBlueprintUiState.Error(
                    e.localizedMessage ?: "Gemini 3B yapı planı oluşturulamadı."
                )
            }
        }
    }

    fun buildGeneratedAiBlueprint(template: StructureTemplate) {
        buildStructureTemplate(template)
    }

    fun showBanner(msg: String) {
        viewModelScope.launch {
            _statusBannerMessage.value = msg
            delay(2800L)
            if (_statusBannerMessage.value == msg) {
                _statusBannerMessage.value = null
            }
        }
    }

    private companion object {
        const val TAG = "BlockMineVM"
    }
}
