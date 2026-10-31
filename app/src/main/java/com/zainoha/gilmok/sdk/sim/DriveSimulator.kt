package com.zainoha.gilmok.sdk.sim

import com.zainoha.gilmok.domain.Geo
import com.zainoha.gilmok.sdk.Gear
import com.zainoha.gilmok.sdk.GeoPoint
import com.zainoha.gilmok.sdk.LocationFix
import com.zainoha.gilmok.sdk.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * 주행 시뮬레이터. 경로선을 따라 좌표가 자동 진행되고, 데모 패널의 속도·정차 조작을 받는다.
 * 길목 근처(slowZones)에서는 실시간, 길목 사이 구간은 fastFactor 배속으로 시간을 돌린다.
 * Vehicle / FusedLocation 시뮬레이터 프로바이더가 여기서 값을 읽어 간다.
 */
class DriveSimulator(scope: CoroutineScope, val points: List<GeoPoint>) {

    val cumulativeM: DoubleArray = DoubleArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + Geo.distanceM(points[i - 1], points[i])
    }
    val totalM: Double get() = cumulativeM.last()

    val traveledM = MutableStateFlow(0.0)
    val targetSpeedKmh = MutableStateFlow(70f)
    val parked = MutableStateFlow(true)
    val fastForward = MutableStateFlow(true)
    val started = MutableStateFlow(false)
    val arrived = MutableStateFlow(false)
    var fastFactor = 60.0
    var slowZones: List<ClosedFloatingPointRange<Double>> = emptyList()

    private val _vehicle = MutableStateFlow(VehicleState())
    val vehicle: StateFlow<VehicleState> = _vehicle
    private val _fix = MutableStateFlow(fixAt(0.0, 0f))
    val fix: StateFlow<LocationFix> = _fix

    private var curSpeed = 0f
    private var continuousSec = 0.0
    private var stillSec = 0.0

    /** 현재 적용 중인 시간 배속 (화면 표시용). */
    val currentScale: Double
        get() = if (fastForward.value && curSpeed > 0.5f && slowZones.none { traveledM.value in it }) fastFactor else 1.0

    init {
        scope.launch {
            while (isActive) {
                step(0.1)
                delay(100)
            }
        }
    }

    private fun step(dtReal: Double) {
        val target = if (!started.value || parked.value || arrived.value) 0f else targetSpeedKmh.value
        curSpeed = if (target > curSpeed) min(target, curSpeed + 30f * dtReal.toFloat())
        else max(target, curSpeed - 45f * dtReal.toFloat())
        if (curSpeed < 0.5f) curSpeed = 0f

        val dt = dtReal * currentScale
        if (curSpeed > 0f) {
            val next = traveledM.value + curSpeed / 3.6 * dt
            if (next >= totalM) {
                traveledM.value = totalM
                arrived.value = true
                parked.value = true
                curSpeed = 0f
            } else {
                traveledM.value = next
            }
            continuousSec += dt
            stillSec = 0.0
        } else {
            stillSec += dtReal
            if (stillSec > 90.0) continuousSec = 0.0 // 1분 30초 이상 서 있으면 연속 주행 시간 초기화
        }
        val gear = if (parked.value || !started.value) Gear.P else Gear.D
        _vehicle.value = VehicleState(curSpeed, gear, continuousSec.toLong())
        _fix.value = fixAt(traveledM.value, curSpeed)
    }

    fun fixAt(distM: Double, speed: Float): LocationFix {
        val (p, h) = positionAt(distM)
        return LocationFix(p, h, speed)
    }

    fun positionAt(distM: Double): Pair<GeoPoint, Float> {
        val d = distM.coerceIn(0.0, totalM)
        var i = 0
        while (i < cumulativeM.size - 2 && cumulativeM[i + 1] < d) i++
        val segLen = (cumulativeM[i + 1] - cumulativeM[i]).coerceAtLeast(1.0)
        val t = ((d - cumulativeM[i]) / segLen).coerceIn(0.0, 1.0)
        val p = Geo.interpolate(points[i], points[i + 1], t)
        return p to Geo.bearing(points[i], points[i + 1])
    }

    /** 경로 꼭짓점과 일치하는 지점의 경로상 거리. 가장 가까운 꼭짓점을 쓴다. */
    fun routeDistanceOf(p: GeoPoint): Double {
        var best = 0
        var bestD = Double.MAX_VALUE
        points.forEachIndexed { i, q ->
            val d = Geo.distanceM(p, q)
            if (d < bestD) { bestD = d; best = i }
        }
        return cumulativeM[best]
    }

    fun start() {
        started.value = true
        arrived.value = false
        parked.value = false
    }

    fun jumpTo(distM: Double) {
        traveledM.value = distM.coerceIn(0.0, totalM - 1.0)
        arrived.value = false
        if (started.value) parked.value = false
        stillSec = 0.0
    }

    fun reset() {
        started.value = false
        parked.value = true
        arrived.value = false
        traveledM.value = 0.0
        targetSpeedKmh.value = 70f
        curSpeed = 0f
        continuousSec = 0.0
        stillSec = 0.0
        _vehicle.value = VehicleState()
        _fix.value = fixAt(0.0, 0f)
    }

    /** 데모용: 연속 주행 시간을 강제로 올린다. */
    fun addContinuousDrivingSec(sec: Long) { continuousSec += sec }
}
