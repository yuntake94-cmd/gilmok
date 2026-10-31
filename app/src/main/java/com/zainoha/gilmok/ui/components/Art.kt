package com.zainoha.gilmok.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainoha.gilmok.content.ArtKind
import com.zainoha.gilmok.sdk.GeoPoint
import com.zainoha.gilmok.ui.theme.G
import kotlin.math.cos
import kotlin.math.sin

/**
 * 길목 플레이스홀더 일러스트. 사진 대신 Canvas 로 직접 그린다 (저작권 문제 없음).
 * 실제 서비스에서는 한국관광공사 TourAPI 의 사진 URL 로 교체할 자리.
 */
@Composable
fun LandmarkArt(kind: ArtKind, modifier: Modifier = Modifier, dim: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                ArtKind.SUSPENSION_BRIDGE -> suspensionBridge()
                ArtKind.CABLE_BRIDGE -> cableBridge()
                ArtKind.ISLANDS -> islands()
                ArtKind.TUNNEL -> tunnel()
                ArtKind.BEACH -> beach()
                ArtKind.CASTLE -> castle()
                ArtKind.GARDEN -> garden()
                ArtKind.REST -> rest()
                ArtKind.SHOP -> shop()
                ArtKind.PARKING -> parking()
            }
            if (dim) drawRect(Color.Black.copy(alpha = 0.55f))
        }
    }
}

private fun DrawScope.skySea(seaTop: Float = 0.62f, skyTop: Color = G.SkyTop, skyBottom: Color = Color(0xFF2A4A7A), sea: Color = G.SeaDeep) {
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom), endY = size.height * seaTop))
    drawRect(Brush.verticalGradient(listOf(sea, Color(0xFF082236)), startY = size.height * seaTop),
        topLeft = Offset(0f, size.height * seaTop), size = Size(size.width, size.height * (1 - seaTop)))
    // 잔물결
    val wave = Color.White.copy(alpha = 0.08f)
    for (i in 0..5) {
        val y = size.height * (seaTop + 0.06f + i * 0.06f)
        drawLine(wave, Offset(size.width * (0.05f + i * 0.08f), y), Offset(size.width * (0.35f + i * 0.08f), y), 2f, StrokeCap.Round)
    }
}

private fun DrawScope.suspensionBridge() {
    skySea(0.7f)
    val deckY = size.height * 0.62f
    val white = Color(0xFFE6ECF4)
    drawLine(white, Offset(0f, deckY), Offset(size.width, deckY), 7f)
    val towers = listOf(0.3f, 0.7f)
    towers.forEach { tx ->
        drawRect(white, Offset(size.width * tx - 7f, size.height * 0.2f), Size(14f, deckY - size.height * 0.2f))
    }
    val cable = Path().apply {
        moveTo(0f, deckY - 10f)
        quadraticTo(size.width * 0.15f, size.height * 0.62f, size.width * 0.3f, size.height * 0.2f)
        quadraticTo(size.width * 0.5f, size.height * 0.72f, size.width * 0.7f, size.height * 0.2f)
        quadraticTo(size.width * 0.85f, size.height * 0.62f, size.width, deckY - 10f)
    }
    drawPath(cable, white, style = Stroke(4f))
    var x = 0.08f
    while (x < 0.93f) {
        val cy = cableY(x)
        drawLine(white.copy(alpha = 0.7f), Offset(size.width * x, size.height * cy), Offset(size.width * x, deckY), 1.5f)
        x += 0.035f
    }
    // 경관 조명 점
    var lx = 0.02f
    while (lx < 1f) { drawCircle(G.Mint.copy(alpha = 0.9f), 2.5f, Offset(size.width * lx, deckY - 14f)); lx += 0.04f }
}

private fun cableY(x: Float): Float = when {
    x < 0.3f -> 0.62f - (0.42f) * (x / 0.3f) * (x / 0.3f)
    x < 0.7f -> { val t = (x - 0.3f) / 0.4f; 0.2f + 0.46f * 4 * t * (1 - t) }
    else -> { val t = (x - 0.7f) / 0.3f; 0.2f + 0.42f * t * t }
}

private fun DrawScope.cableBridge() {
    skySea(0.72f)
    val deckY = size.height * 0.66f
    val white = Color(0xFFE6ECF4)
    drawLine(white, Offset(0f, deckY), Offset(size.width, deckY), 7f)
    listOf(0.28f, 0.72f).forEach { tx ->
        val top = size.height * 0.14f
        drawRect(white, Offset(size.width * tx - 8f, top), Size(16f, deckY - top))
        for (i in 1..10) {
            val dx = size.width * (0.03f + i * 0.021f)
            drawLine(white.copy(alpha = 0.75f), Offset(size.width * tx, top + i * 6f), Offset(size.width * tx - dx, deckY), 1.6f)
            drawLine(white.copy(alpha = 0.75f), Offset(size.width * tx, top + i * 6f), Offset(size.width * tx + dx, deckY), 1.6f)
        }
    }
}

private fun DrawScope.islands() {
    skySea(0.55f, skyTop = Color(0xFF263A63), skyBottom = Color(0xFF5B6F9E))
    val rock = Color(0xFF3B3F4C)
    val rockLight = Color(0xFF5A5F6E)
    val xs = listOf(0.12f, 0.27f, 0.4f, 0.56f, 0.7f, 0.86f)
    val hs = listOf(0.12f, 0.18f, 0.14f, 0.2f, 0.11f, 0.16f)
    val ws = listOf(0.07f, 0.09f, 0.07f, 0.1f, 0.06f, 0.09f)
    val baseY = size.height * 0.57f
    xs.forEachIndexed { i, cx ->
        val p = Path().apply {
            moveTo(size.width * (cx - ws[i]), baseY)
            lineTo(size.width * (cx - ws[i] * 0.4f), baseY - size.height * hs[i])
            lineTo(size.width * (cx + ws[i] * 0.3f), baseY - size.height * hs[i] * 0.85f)
            lineTo(size.width * (cx + ws[i]), baseY)
            close()
        }
        drawPath(p, if (i % 2 == 0) rock else rockLight)
    }
    // 등대섬 등대
    val lx = size.width * 0.86f
    drawRect(Color(0xFFF4F6FA), Offset(lx - 5f, baseY - size.height * 0.26f), Size(10f, size.height * 0.11f))
    drawCircle(G.Orange, 6f, Offset(lx, baseY - size.height * 0.27f))
}

private fun DrawScope.tunnel() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2C44), Color(0xFF071A2A))))
    val cx = size.width / 2; val cy = size.height * 0.56f
    for (i in 8 downTo 1) {
        val r = size.height * 0.08f * i
        val ring = Path().apply {
            addArc(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r * 0.9f), 180f, 180f)
        }
        drawPath(ring, Color(0xFF9AA3B2).copy(alpha = 0.25f + 0.05f * (8 - i)), style = Stroke(3f))
        drawLine(Color(0xFF9AA3B2).copy(alpha = 0.3f), Offset(cx - r, cy), Offset(cx - r, cy + r * 0.5f), 3f)
        drawLine(Color(0xFF9AA3B2).copy(alpha = 0.3f), Offset(cx + r, cy), Offset(cx + r, cy + r * 0.5f), 3f)
        drawCircle(G.Orange.copy(alpha = 0.9f), 3f + i * 0.4f, Offset(cx, cy - r + 10f))
    }
    // 노면
    drawRect(Color(0xFF1A2230), Offset(0f, size.height * 0.8f), Size(size.width, size.height * 0.2f))
    val road = Path().apply { moveTo(cx - size.width * 0.5f, size.height); lineTo(cx - 40f, cy + 40f); lineTo(cx + 40f, cy + 40f); lineTo(cx + size.width * 0.5f, size.height); close() }
    drawPath(road, Color(0xFF222A38))
    drawLine(Color(0xFFE6ECF4).copy(alpha = 0.6f), Offset(cx, cy + 40f), Offset(cx, size.height), 3f)
}

private fun DrawScope.beach() {
    skySea(0.5f, skyTop = Color(0xFF2D4A7A), skyBottom = Color(0xFFE3A565), sea = Color(0xFF1E6F8E))
    drawCircle(Color(0xFFFFD08A), size.height * 0.1f, Offset(size.width * 0.72f, size.height * 0.36f))
    val sand = Path().apply {
        moveTo(0f, size.height); lineTo(0f, size.height * 0.78f)
        quadraticTo(size.width * 0.5f, size.height * 0.64f, size.width, size.height * 0.8f)
        lineTo(size.width, size.height); close()
    }
    drawPath(sand, Color(0xFFE9D6B0))
    for (i in 0..4) {
        val x = size.width * (0.1f + i * 0.2f)
        drawLine(Color(0xFF3F4E68), Offset(x, size.height * 0.86f), Offset(x, size.height * 0.74f), 3f)
        drawCircle(G.Orange.copy(alpha = 0.9f), size.height * 0.06f, Offset(x, size.height * 0.72f))
    }
}

private fun DrawScope.castle() {
    skySea(0.6f)
    val stone = Color(0xFF8A8F99)
    val dark = Color(0xFF5C616C)
    val baseY = size.height * 0.88f
    drawRect(stone, Offset(size.width * 0.08f, size.height * 0.42f), Size(size.width * 0.84f, baseY - size.height * 0.42f))
    for (i in 0..10) {
        val x = size.width * (0.08f + i * 0.084f)
        drawRect(if (i % 2 == 0) stone else dark, Offset(x, size.height * 0.34f), Size(size.width * 0.042f, size.height * 0.09f))
    }
    listOf(0.2f, 0.5f, 0.8f).forEach { cx ->
        val w = size.width * 0.14f; val top = size.height * 0.52f; val bottom = baseY
        val arch = Path().apply {
            moveTo(size.width * cx - w / 2, bottom); lineTo(size.width * cx - w / 2, top + w / 2)
            arcTo(androidx.compose.ui.geometry.Rect(size.width * cx - w / 2, top, size.width * cx + w / 2, top + w), 180f, 180f, false)
            lineTo(size.width * cx + w / 2, bottom); close()
        }
        drawPath(arch, Brush.verticalGradient(listOf(Color(0xFF2A4A7A), G.SeaDeep)))
        // 아치 너머 거가대교 실루엣
        drawLine(Color(0xFFE6ECF4).copy(alpha = 0.8f), Offset(size.width * cx - w / 2, top + w * 0.95f), Offset(size.width * cx + w / 2, top + w * 0.95f), 3f)
        drawLine(Color(0xFFE6ECF4).copy(alpha = 0.8f), Offset(size.width * cx, top + w * 0.35f), Offset(size.width * cx, top + w * 0.95f), 3f)
    }
    var y = size.height * 0.46f
    while (y < baseY) { drawLine(dark.copy(alpha = 0.5f), Offset(size.width * 0.08f, y), Offset(size.width * 0.92f, y), 1.5f); y += size.height * 0.07f }
}

private fun DrawScope.garden() {
    skySea(0.62f, skyTop = Color(0xFF203B6A), skyBottom = Color(0xFF6E8AC2), sea = Color(0xFF14628A))
    val island = Path().apply {
        moveTo(size.width * 0.1f, size.height * 0.66f)
        quadraticTo(size.width * 0.5f, size.height * 0.3f, size.width * 0.9f, size.height * 0.66f)
        close()
    }
    drawPath(island, Color(0xFF2F8F5B))
    listOf(0.25f, 0.38f, 0.5f, 0.62f, 0.75f).forEachIndexed { i, cx ->
        val r = size.height * (0.07f + (i % 2) * 0.03f)
        val cy = size.height * (0.52f - (if (i == 2) 0.12f else if (i == 1 || i == 3) 0.07f else 0f))
        drawCircle(if (i % 2 == 0) Color(0xFF3FB36F) else Color(0xFF1F7A48), r, Offset(size.width * cx, cy))
        drawRect(Color(0xFF5B3A1E), Offset(size.width * cx - 3f, cy), Size(6f, r * 1.1f))
    }
    for (i in 0..5) {
        val y = size.height * (0.44f + i * 0.04f)
        drawLine(Color(0xFFF4F6FA).copy(alpha = 0.9f), Offset(size.width * (0.5f - i * 0.012f), y), Offset(size.width * (0.5f + i * 0.012f), y), 3f)
    }
    // 꽃점
    for (i in 0..14) {
        val a = i * 0.43f
        drawCircle(if (i % 3 == 0) Color(0xFFFF7FA8) else G.Orange, 3.5f,
            Offset(size.width * (0.5f + 0.33f * cos(a)), size.height * (0.6f - 0.1f * sin(a) * sin(a))))
    }
}

private fun DrawScope.rest() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF203B6A), Color(0xFF0E3A5B))))
    val w = size.width; val h = size.height
    drawRoundRect(Color(0xFFF4F6FA), Offset(w * 0.34f, h * 0.4f), Size(w * 0.26f, h * 0.32f), CornerRadius(12f, 12f))
    drawArc(Color(0xFFF4F6FA), -90f, 180f, false, Offset(w * 0.56f, h * 0.44f), Size(w * 0.1f, h * 0.18f), style = Stroke(8f))
    for (i in 0..2) {
        val x = w * (0.4f + i * 0.07f)
        val steam = Path().apply { moveTo(x, h * 0.36f); quadraticTo(x + 8f, h * 0.3f, x, h * 0.24f); quadraticTo(x - 8f, h * 0.18f, x, h * 0.12f) }
        drawPath(steam, G.Mint.copy(alpha = 0.8f), style = Stroke(3f, cap = StrokeCap.Round))
    }
    drawLine(Color(0xFFF4F6FA).copy(alpha = 0.5f), Offset(w * 0.28f, h * 0.8f), Offset(w * 0.72f, h * 0.8f), 5f, StrokeCap.Round)
}

private fun DrawScope.shop() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2F3A), Color(0xFF1A1E27))))
    val w = size.width; val h = size.height
    drawRect(Color(0xFF4A4F5E), Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.45f))
    for (i in 0..5) {
        drawRect(if (i % 2 == 0) G.Orange else Color(0xFFF4F6FA), Offset(w * (0.16f + i * 0.113f), h * 0.3f), Size(w * 0.113f, h * 0.12f))
    }
    drawRect(Color(0xFF9AD1FF).copy(alpha = 0.6f), Offset(w * 0.26f, h * 0.5f), Size(w * 0.2f, h * 0.22f))
    drawRect(Color(0xFF2A1A00), Offset(w * 0.56f, h * 0.52f), Size(w * 0.16f, h * 0.33f))
    drawRoundRect(G.Mint, Offset(w * 0.3f, h * 0.76f), Size(w * 0.12f, h * 0.07f), CornerRadius(6f, 6f))
}

private fun DrawScope.parking() {
    skySea(0.5f, sea = Color(0xFF1E6F8E))
    val w = size.width; val h = size.height
    drawRect(Color(0xFF2A2F3A), Offset(0f, h * 0.7f), Size(w, h * 0.3f))
    for (i in 0..4) drawLine(Color(0xFFF4F6FA).copy(alpha = 0.6f), Offset(w * (0.12f + i * 0.19f), h * 0.72f), Offset(w * (0.12f + i * 0.19f), h * 0.98f), 3f)
    drawRoundRect(G.Mint, Offset(w * 0.42f, h * 0.76f), Size(w * 0.16f, h * 0.17f), CornerRadius(10f, 10f))
    drawRoundRect(Color(0xFF1E6F8E), Offset(w * 0.45f, h * 0.78f), Size(w * 0.1f, h * 0.06f), CornerRadius(4f, 4f))
}

private val labeledLandmarks = setOf("광안대교", "가덕해저터널", "매미성", "외도 보타니아")

/**
 * 경로 미니맵. 경로선 전체를 상자에 맞춰 그리고, 지나온 구간은 민트, 남은 구간은 회색,
 * 길목은 주황 점, 현재 위치는 민트 원 + 진행 방향 화살표로 표시한다.
 */
@Composable
fun RouteMap(
    points: List<GeoPoint>,
    cumulativeM: DoubleArray,
    traveledM: Double,
    current: GeoPoint,
    headingDeg: Float,
    landmarks: List<Pair<String, GeoPoint>>,
    landmarkDistM: Map<String, Double>,
    waypoints: List<GeoPoint>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val minLat = points.minOf { it.lat }; val maxLat = points.maxOf { it.lat }
        val minLon = points.minOf { it.lon }; val maxLon = points.maxOf { it.lon }
        val pad = 34f
        val latScale = (size.height - 2 * pad) / (maxLat - minLat).toFloat()
        val lonScale = (size.width - 2 * pad) / ((maxLon - minLon).toFloat() * cos(Math.toRadians((minLat + maxLat) / 2)).toFloat())
        val scale = minOf(latScale, lonScale)
        val cosLat = cos(Math.toRadians((minLat + maxLat) / 2)).toFloat()
        val wUsed = (maxLon - minLon).toFloat() * cosLat * scale
        val hUsed = (maxLat - minLat).toFloat() * scale
        val ox = (size.width - wUsed) / 2; val oy = (size.height - hUsed) / 2
        fun toXY(p: GeoPoint) = Offset(ox + ((p.lon - minLon).toFloat() * cosLat) * scale, oy + hUsed - (p.lat - minLat).toFloat() * scale)

        val xy = points.map { toXY(it) }
        for (i in 1 until xy.size) {
            val done = cumulativeM[i] <= traveledM
            drawLine(if (done) G.Mint else Color(0xFF4A5160), xy[i - 1], xy[i], 9f, StrokeCap.Round)
        }
        landmarks.forEach { (id, p) ->
            val passed = (landmarkDistM[id] ?: 0.0) < traveledM - 300
            drawCircle(if (passed) Color(0xFF6B7280) else G.Orange, 10f, toXY(p))
        }
        waypoints.forEach { p -> drawCircle(G.Orange, 15f, toXY(p), style = Stroke(4f)) }
        val c = toXY(current)
        drawCircle(G.Mint, 22f, c)
        drawCircle(G.Bg, 22f, c, style = Stroke(3f))
        val a = Math.toRadians(headingDeg.toDouble())
        val tip = Offset(c.x + 13f * sin(a).toFloat(), c.y - 13f * cos(a).toFloat())
        val l = Offset(c.x + 10f * sin(a + 2.5).toFloat(), c.y - 10f * cos(a + 2.5).toFloat())
        val r = Offset(c.x + 10f * sin(a - 2.5).toFloat(), c.y - 10f * cos(a - 2.5).toFloat())
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(c.x, c.y); lineTo(r.x, r.y); close() }, G.Bg)
        if (showLabels) {
            // 라벨은 겹치지 않는 대표 길목만. 오른쪽 가장자리 근처는 점 왼쪽에 쓴다.
            val style = TextStyle(color = G.Text, fontSize = 16.sp)
            landmarks.filter { (id, _) -> id in labeledLandmarks }.forEach { (id, p) ->
                val o = toXY(p)
                val layout = measurer.measure(id, style, softWrap = false)
                val x = if (o.x > size.width * 0.6f) o.x - layout.size.width - 16f else o.x + 16f
                val y = (o.y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height)
                drawText(layout, topLeft = Offset(x, y))
            }
        }
    }
}
