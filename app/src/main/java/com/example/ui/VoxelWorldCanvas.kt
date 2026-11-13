package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.BlockCatalog
import com.example.data.GraphicsAndUiConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private data class ProjectedPolygon(
    val depth: Float,
    val path: Path,
    val fillColor: Color,
    val strokeColor: Color?,
    val isTargeted: Boolean = false,
    val isGrassSide: Boolean = false,
    val grassTopColor: Color? = null,
    val isEmissive: Boolean = false,
    val p0: Offset = Offset.Zero,
    val p1: Offset = Offset.Zero
)

/**
 * Hardware-accelerated 3D Perspective Voxel Renderer for BLOCKMİNE.
 * Supports 360-degree touch camera look, tap-to-place/break, HD procedural grass overhang detail,
 * emissive diamond/portal highlights, dynamic sky presets, multiplayer avatars, and 2D minimap.
 */
@Composable
fun VoxelWorldCanvas(
    worldBlocks: Map<VoxelCoord, Int>,
    playerX: Float,
    playerY: Float,
    playerZ: Float,
    playerYaw: Float,
    playerPitch: Float,
    targetedRaycast: RaycastHit?,
    onlinePeers: List<OnlinePeerAvatar>,
    graphicsConfig: GraphicsAndUiConfig,
    onRotateCamera: (Float, Float) -> Unit,
    onCanvasTapAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleRadius = graphicsConfig.quality.renderRadius

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("voxel_world_canvas")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onRotateCamera(dragAmount.x * 0.0055f, -dragAmount.y * 0.0045f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onCanvasTapAction() }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Sky Gradient & Atmospheric Backdrop
        val skyTop = Color(graphicsConfig.weather.skyTop)
        val skyBottom = Color(graphicsConfig.weather.skyBottom)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyBottom)
            ),
            size = size
        )

        // Draw blocky voxel sun/moon & pixel clouds
        val sunScreenX = ((w * 0.5f) - sin(playerYaw) * w * 0.4f).coerceIn(60f, w - 60f)
        val sunScreenY = (h * 0.18f + sin(playerPitch) * h * 0.25f).coerceIn(40f, h * 0.45f)
        drawRect(
            color = Color(0x55FFF3B0),
            topLeft = Offset(sunScreenX - 42f, sunScreenY - 42f),
            size = Size(84f, 84f)
        )
        drawRect(
            color = Color(0xFFFFEA79),
            topLeft = Offset(sunScreenX - 26f, sunScreenY - 26f),
            size = Size(52f, 52f)
        )

        // 2. 3D Camera Projection Math
        val sinYaw = sin(playerYaw)
        val cosYaw = cos(playerYaw)
        val sinPitch = sin(playerPitch)
        val cosPitch = cos(playerPitch)
        val fovRad = Math.toRadians(graphicsConfig.fov.toDouble()).toFloat()
        val focal = (minOf(w, h) * 0.5f) / tan(fovRad * 0.5f)
        val cx = w * 0.5f
        val cy = h * 0.5f

        fun projectVertex(vx: Float, vy: Float, vz: Float): Triple<Float, Float, Float>? {
            val dx = vx - playerX
            val dy = vy - playerY
            val dz = vz - playerZ

            // Rotate by yaw around Y axis
            val rx = dx * cosYaw - dz * sinYaw
            val rz1 = dx * sinYaw + dz * cosYaw

            // Rotate by pitch around X axis
            val ry = dy * cosPitch + rz1 * sinPitch
            val rz = -dy * sinPitch + rz1 * cosPitch

            if (rz < 0.25f) return null
            val sx = cx + (rx / rz) * focal
            val sy = cy - (ry / rz) * focal
            return Triple(sx, sy, rz)
        }

        val polygons = ArrayList<ProjectedPolygon>(512)
        val pxInt = playerX.roundToInt()
        val pzInt = playerZ.roundToInt()
        val targetCoord = targetedRaycast?.hitCoord

        // 3. Collect Visible Block Faces (Painter's Algorithm + Backface & Neighbor Culling)
        for ((coord, blockId) in worldBlocks) {
            if (abs(coord.x - pxInt) > visibleRadius || abs(coord.z - pzInt) > visibleRadius) continue

            val distSq = (coord.x - playerX) * (coord.x - playerX) +
                (coord.y - playerY) * (coord.y - playerY) +
                (coord.z - playerZ) * (coord.z - playerZ)
            if (distSq > (visibleRadius + 3) * (visibleRadius + 3)) continue

            val blockType = BlockCatalog.byId(blockId)
            val isTarget = (coord == targetCoord)
            val x0 = coord.x - 0.5f
            val x1 = coord.x + 0.5f
            val y0 = coord.y - 0.5f
            val y1 = coord.y + 0.5f
            val z0 = coord.z - 0.5f
            val z1 = coord.z + 0.5f

            val topCol = Color(blockType.topColor)
            val sideCol = Color(blockType.sideColor)
            val shadedSideCol = Color(
                red = (sideCol.red * 0.82f).coerceIn(0f, 1f),
                green = (sideCol.green * 0.82f).coerceIn(0f, 1f),
                blue = (sideCol.blue * 0.82f).coerceIn(0f, 1f),
                alpha = if (blockType.isTranslucent) 0.72f else 1f
            )
            val brightSideCol = sideCol.copy(alpha = if (blockType.isTranslucent) 0.75f else 1f)

            fun addQuad(
                v0: Triple<Float, Float, Float>?,
                v1: Triple<Float, Float, Float>?,
                v2: Triple<Float, Float, Float>?,
                v3: Triple<Float, Float, Float>?,
                color: Color,
                grassSide: Boolean = false
            ) {
                if (v0 == null || v1 == null || v2 == null || v3 == null) return
                val avgDepth = (v0.third + v1.third + v2.third + v3.third) * 0.25f
                val path = Path().apply {
                    moveTo(v0.first, v0.second)
                    lineTo(v1.first, v1.second)
                    lineTo(v2.first, v2.second)
                    lineTo(v3.first, v3.second)
                    close()
                }
                polygons.add(
                    ProjectedPolygon(
                        depth = avgDepth,
                        path = path,
                        fillColor = color,
                        strokeColor = if (isTarget) Color(0xFFFFF254) else Color(0x28000000),
                        isTargeted = isTarget,
                        isGrassSide = grassSide && blockId == 1,
                        grassTopColor = if (blockId == 1) topCol else null,
                        isEmissive = blockType.isEmissive,
                        p0 = Offset(v0.first, v0.second),
                        p1 = Offset(v1.first, v1.second)
                    )
                )
            }

            // Top Face (+Y)
            if (playerY >= y1 - 0.2f && !worldBlocks.containsKey(VoxelCoord(coord.x, coord.y + 1, coord.z))) {
                addQuad(
                    projectVertex(x0, y1, z0),
                    projectVertex(x1, y1, z0),
                    projectVertex(x1, y1, z1),
                    projectVertex(x0, y1, z1),
                    topCol.copy(alpha = if (blockType.isTranslucent) 0.78f else 1f)
                )
            }
            // Front/South Face (-Z)
            if (playerZ <= z0 + 0.2f && !worldBlocks.containsKey(VoxelCoord(coord.x, coord.y, coord.z - 1))) {
                addQuad(
                    projectVertex(x0, y1, z0),
                    projectVertex(x1, y1, z0),
                    projectVertex(x1, y0, z0),
                    projectVertex(x0, y0, z0),
                    brightSideCol,
                    grassSide = true
                )
            }
            // Back/North Face (+Z)
            if (playerZ >= z1 - 0.2f && !worldBlocks.containsKey(VoxelCoord(coord.x, coord.y, coord.z + 1))) {
                addQuad(
                    projectVertex(x1, y1, z1),
                    projectVertex(x0, y1, z1),
                    projectVertex(x0, y0, z1),
                    projectVertex(x1, y0, z1),
                    shadedSideCol,
                    grassSide = true
                )
            }
            // Left/West Face (-X)
            if (playerX <= x0 + 0.2f && !worldBlocks.containsKey(VoxelCoord(coord.x - 1, coord.y, coord.z))) {
                addQuad(
                    projectVertex(x0, y1, z1),
                    projectVertex(x0, y1, z0),
                    projectVertex(x0, y0, z0),
                    projectVertex(x0, y0, z1),
                    shadedSideCol,
                    grassSide = true
                )
            }
            // Right/East Face (+X)
            if (playerX >= x1 - 0.2f && !worldBlocks.containsKey(VoxelCoord(coord.x + 1, coord.y, coord.z))) {
                addQuad(
                    projectVertex(x1, y1, z0),
                    projectVertex(x1, y1, z1),
                    projectVertex(x1, y0, z1),
                    projectVertex(x1, y0, z0),
                    brightSideCol,
                    grassSide = true
                )
            }
        }

        // Sort far-to-near (Painter's algorithm)
        polygons.sortByDescending { it.depth }

        for (poly in polygons) {
            drawPath(path = poly.path, color = poly.fillColor)
            if (poly.isGrassSide && poly.grassTopColor != null) {
                drawLine(
                    color = poly.grassTopColor,
                    start = poly.p0,
                    end = poly.p1,
                    strokeWidth = (18f / poly.depth).coerceIn(2f, 10f)
                )
            }
            if (poly.strokeColor != null) {
                drawPath(
                    path = poly.path,
                    color = poly.strokeColor,
                    style = Stroke(width = if (poly.isTargeted) 3.5f else 1f)
                )
            }
        }

        // 4. Render Online Multiplayer Companion Avatars in 3D Space
        for (peer in onlinePeers) {
            val projected = projectVertex(peer.x, peer.y + 0.9f, peer.z) ?: continue
            val sx = projected.first
            val sy = projected.second
            val depth = projected.third
            if (sx in 0f..w && sy in 0f..h && depth in 0.8f..18f) {
                val scale = (140f / depth).coerceIn(14f, 68f)
                // Shadow
                drawCircle(
                    color = Color(0x55000000),
                    radius = scale * 0.45f,
                    center = Offset(sx, sy + scale * 0.85f)
                )
                // Legs
                drawRect(
                    color = Color(0xFF1D3557),
                    topLeft = Offset(sx - scale * 0.28f, sy + scale * 0.25f),
                    size = Size(scale * 0.56f, scale * 0.55f)
                )
                // Tunic / Torso
                drawRect(
                    color = Color(peer.tunicColor),
                    topLeft = Offset(sx - scale * 0.35f, sy - scale * 0.3f),
                    size = Size(scale * 0.7f, scale * 0.6f)
                )
                // Voxel Head
                drawRect(
                    color = Color(0xFFF4A261),
                    topLeft = Offset(sx - scale * 0.3f, sy - scale * 0.88f),
                    size = Size(scale * 0.6f, scale * 0.58f)
                )
                // Hair
                drawRect(
                    color = Color(0xFF3B271A),
                    topLeft = Offset(sx - scale * 0.32f, sy - scale * 0.92f),
                    size = Size(scale * 0.64f, scale * 0.18f)
                )
                // Online indicator dot above head
                drawCircle(
                    color = Color(0xFF5EBD3E),
                    radius = (scale * 0.14f).coerceAtLeast(4f),
                    center = Offset(sx, sy - scale * 1.12f)
                )
            }
        }

        // 5. Render First-Person Equipped Block / Tool in Bottom-Right Hand
        drawFirstPersonHand(w, h)

        // 6. Render 2D Minimap Radar in Top-Right if enabled
        if (graphicsConfig.showMinimap) {
            drawMinimapRadar(
                worldBlocks = worldBlocks,
                playerX = playerX,
                playerZ = playerZ,
                playerYaw = playerYaw,
                onlinePeers = onlinePeers,
                canvasWidth = w
            )
        }
    }
}

private fun DrawScope.drawFirstPersonHand(w: Float, h: Float) {
    val handW = (w * 0.11f).coerceIn(56f, 110f)
    val handH = handW * 1.15f
    val left = w - handW * 1.45f
    val top = h - handH * 1.25f

    // Isometric voxel block held in hand
    drawRect(
        color = Color(0xFF5EBD3E),
        topLeft = Offset(left, top),
        size = Size(handW, handH * 0.32f)
    )
    drawRect(
        color = Color(0xFF866043),
        topLeft = Offset(left, top + handH * 0.32f),
        size = Size(handW, handH * 0.68f)
    )
    drawRect(
        color = Color(0xFF1B1B1B),
        topLeft = Offset(left, top),
        size = Size(handW, handH),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawMinimapRadar(
    worldBlocks: Map<VoxelCoord, Int>,
    playerX: Float,
    playerZ: Float,
    playerYaw: Float,
    onlinePeers: List<OnlinePeerAvatar>,
    canvasWidth: Float
) {
    val mapSize = 112f
    val padRight = 16f
    val padTop = 94f
    val originX = canvasWidth - mapSize - padRight
    val originY = padTop

    // Minimap background frame
    drawRect(
        color = Color(0xCC0D1B2A),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f)
    )
    drawRect(
        color = Color(0xFF5EBD3E),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f),
        style = Stroke(width = 2f)
    )

    val cellSize = mapSize / 17f
    for (gx in -8..8) {
        for (gz in -8..8) {
            var topBlockId = 1
            for (y in 12 downTo 0) {
                val id = worldBlocks[VoxelCoord(gx, y, gz)]
                if (id != null) {
                    topBlockId = id
                    break
                }
            }
            val col = Color(BlockCatalog.byId(topBlockId).topColor)
            val cx = originX + (gx + 8) * cellSize
            val cz = originY + (gz + 8) * cellSize
            drawRect(
                color = col,
                topLeft = Offset(cx, cz),
                size = Size(cellSize + 0.5f, cellSize + 0.5f)
            )
        }
    }

    // Online peer dots on minimap
    for (peer in onlinePeers) {
        val px = originX + ((peer.x + 8f) / 17f) * mapSize
        val pz = originY + ((peer.z + 8f) / 17f) * mapSize
        drawCircle(
            color = Color(0xFF00F5D4),
            radius = 3.5f,
            center = Offset(px.coerceIn(originX, originX + mapSize), pz.coerceIn(originY, originY + mapSize))
        )
    }

    // Player indicator + direction vector
    val myMapX = (originX + ((playerX + 8f) / 17f) * mapSize).coerceIn(originX + 4f, originX + mapSize - 4f)
    val myMapZ = (originY + ((playerZ + 8f) / 17f) * mapSize).coerceIn(originY + 4f, originY + mapSize - 4f)
    drawCircle(
        color = Color(0xFFFFF254),
        radius = 4.5f,
        center = Offset(myMapX, myMapZ)
    )
    drawLine(
        color = Color.White,
        start = Offset(myMapX, myMapZ),
        end = Offset(myMapX + sin(playerYaw) * 10f, myMapZ + cos(playerYaw) * 10f),
        strokeWidth = 2f
    )
}
