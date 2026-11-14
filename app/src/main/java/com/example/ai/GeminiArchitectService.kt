package com.example.ai

import com.example.data.AppLanguage
import com.example.data.BlockCatalog
import com.example.data.StructureTemplate
import com.example.data.VoxelPlacement
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONObject

sealed interface GeminiChatUiState {
    data object Idle : GeminiChatUiState
    data object Loading : GeminiChatUiState
    data class Streaming(val text: String) : GeminiChatUiState
    data class Success(val text: String) : GeminiChatUiState
    data class Error(val message: String) : GeminiChatUiState
}

sealed interface GeminiBlueprintUiState {
    data object Idle : GeminiBlueprintUiState
    data object Loading : GeminiBlueprintUiState
    data class Success(val template: StructureTemplate) : GeminiBlueprintUiState
    data class Error(val message: String) : GeminiBlueprintUiState
}

data class GeminiChatTurn(
    val isUser: Boolean,
    val text: String
)

class GeminiArchitectService {

    private val blueprintSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Short creative name for the 3D voxel structure"),
                "description" to Schema.string(description = "Brief 1-sentence description of the structure"),
                "iconEmoji" to Schema.string(description = "A single matching emoji icon"),
                "blocks" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "dx" to Schema.integer(description = "Relative X offset between -4 and 4"),
                            "dy" to Schema.integer(description = "Relative Y height offset between 0 and 8"),
                            "dz" to Schema.integer(description = "Relative Z offset between -4 and 4"),
                            "blockId" to Schema.integer(description = "Valid block catalog ID from 1 to 24")
                        )
                    ),
                    description = "Between 15 and 90 voxel blocks forming the 3D structure"
                )
            )
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val blueprintModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = blueprintSchema
            }
        )
    }

    fun streamAssistantReply(
        userPrompt: String,
        language: AppLanguage,
        worldBlockCount: Int,
        playerLevel: Long,
        activeServerName: String
    ): Flow<String> {
        val langLabel = when (language) {
            AppLanguage.TR -> "Turkish"
            AppLanguage.EN -> "English"
            AppLanguage.DE -> "German"
            AppLanguage.ES -> "Spanish"
            AppLanguage.JA -> "Japanese"
        }
        val blockSummary = BlockCatalog.allBlocks.joinToString(", ") { "${it.id}:${it.nameEn}" }
        val prompt = """
            You are the BLOCKMİNE 3D Voxel Sandbox AI Architect & World Guide.
            Respond strictly in $langLabel. Keep answers practical, engaging, and concise (max 150 words unless asked for details).
            Player Context: Level $playerLevel, Active Server "$activeServerName", World Blocks Loaded: $worldBlockCount.
            Available Block IDs: $blockSummary.

            Player Question: $userPrompt
        """.trimIndent()

        return assistantModel.generateContentStream(prompt)
            .mapNotNull { chunk -> chunk.text }
    }

    suspend fun generateStructureBlueprint(
        userPrompt: String,
        language: AppLanguage
    ): StructureTemplate {
        val langLabel = when (language) {
            AppLanguage.TR -> "Turkish"
            AppLanguage.EN -> "English"
            AppLanguage.DE -> "German"
            AppLanguage.ES -> "Spanish"
            AppLanguage.JA -> "Japanese"
        }
        val paletteGuide = BlockCatalog.allBlocks.joinToString(", ") { "${it.id}=${it.nameEn}" }
        val fullPrompt = """
            Design a recognizable, compact 3D voxel building for BLOCKMİNE matching this request: "$userPrompt".
            Write name and description in $langLabel.
            Constraints:
            - dx and dz MUST be integers between -4 and 4
            - dy MUST be an integer between 0 and 8
            - blockId MUST be a valid ID from: $paletteGuide
            - Include between 20 and 85 blocks forming a cohesive 3D structure (walls, floor, roof, windows, or glowing accents).
        """.trimIndent()

        val response = blueprintModel.generateContent(fullPrompt)
        val jsonString = response.text ?: throw IllegalStateException("Gemini returned an empty blueprint.")
        val json = JSONObject(jsonString)

        val name = json.optString("name", "AI Yapısı").ifBlank { "AI Yapısı" }
        val description = json.optString("description", userPrompt).ifBlank { userPrompt }
        val iconEmoji = json.optString("iconEmoji", "🤖").ifBlank { "🤖" }
        val blocksArray = json.getJSONArray("blocks")

        val validIds = BlockCatalog.allBlocks.map { it.id }.toSet()
        val placements = mutableListOf<VoxelPlacement>()
        for (i in 0 until blocksArray.length()) {
            val item = blocksArray.getJSONObject(i)
            val dx = item.getInt("dx").coerceIn(-5, 5)
            val dy = item.getInt("dy").coerceIn(0, 10)
            val dz = item.getInt("dz").coerceIn(-5, 5)
            val rawId = item.getInt("blockId")
            val blockId = if (rawId in validIds) rawId else 6
            placements.add(VoxelPlacement(dx = dx, dy = dy, dz = dz, blockId = blockId))
        }

        if (placements.isEmpty()) {
            throw IllegalStateException("Blueprint contained 0 voxel blocks.")
        }

        val uniquePlacements = placements.distinctBy { Triple(it.dx, it.dy, it.dz) }
        return StructureTemplate(
            id = "ai_${System.currentTimeMillis()}",
            nameTr = name,
            nameEn = name,
            descriptionTr = description,
            descriptionEn = description,
            iconEmoji = iconEmoji,
            blocksCount = uniquePlacements.size,
            rewardXp = (uniquePlacements.size * 4).coerceIn(100, 400),
            placements = uniquePlacements
        )
    }
}
