package com.zainoha.gilmok.domain

import com.zainoha.gilmok.sdk.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val R = 6371000.0

    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    /** a 에서 b 를 바라보는 방위각 (0~360, 북=0, 시계방향). */
    fun bearing(a: GeoPoint, b: GeoPoint): Float {
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val y = sin(dLon) * cos(la2)
        val x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    fun interpolate(a: GeoPoint, b: GeoPoint, t: Double): GeoPoint =
        GeoPoint(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)

    /**
     * 차량 진행 방향 기준 상대 방위(0~360)와 사람 말로 된 방향 라벨.
     * 예: (70f, 오른쪽 2시 방향), (300f, 왼쪽 10시 방향)
     */
    fun relative(from: GeoPoint, headingDeg: Float, to: GeoPoint): Pair<Float, String> {
        val b = bearing(from, to)
        val rel = ((b - headingDeg) % 360f + 360f) % 360f
        val hour = (rel / 30f).roundToInt().let { if (it == 0 || it == 12) 12 else it }
        val label = when {
            rel < 15f || rel > 345f -> "정면 12시 방향"
            rel in 165f..195f -> "뒤쪽 6시 방향"
            rel < 180f -> "오른쪽 ${hour}시 방향"
            else -> "왼쪽 ${hour}시 방향"
        }
        return rel to label
    }
}
