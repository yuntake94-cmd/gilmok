package com.zainoha.gilmok.sdk.sim

import com.zainoha.gilmok.sdk.LocationFix
import com.zainoha.gilmok.sdk.LocationProvider
import com.zainoha.gilmok.sdk.NaviProvider
import com.zainoha.gilmok.sdk.RoutePlace
import com.zainoha.gilmok.sdk.RouteState
import com.zainoha.gilmok.sdk.SttProvider
import com.zainoha.gilmok.sdk.VehicleProvider
import com.zainoha.gilmok.sdk.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Vehicle SDK 자리. 시뮬레이터의 속도·기어·연속 주행 시간을 그대로 흘려보낸다. */
class SimVehicleProvider(sim: DriveSimulator) : VehicleProvider {
    override val state: StateFlow<VehicleState> = sim.vehicle
}

/** FusedLocation SDK 자리. 경로선 위 보간 좌표와 세그먼트 방위를 헤딩으로 준다. */
class SimLocationProvider(sim: DriveSimulator) : LocationProvider {
    override val fix: StateFlow<LocationFix> = sim.fix
}

/** NaviHelper SDK 자리. 목적지·경유지·경로선·남은 거리. addWaypoint 는 목록에 넣고 성공을 돌려준다. */
class SimNaviProvider(scope: CoroutineScope, private val sim: DriveSimulator, destination: RoutePlace) : NaviProvider {
    private val waypoints = MutableStateFlow<List<RoutePlace>>(emptyList())

    override val route: StateFlow<RouteState> =
        combine(sim.traveledM, waypoints, sim.arrived) { traveled, wps, arrived ->
            RouteState(destination, wps, sim.points, traveled, sim.totalM, arrived)
        }.stateIn(scope, SharingStarted.Eagerly, RouteState(destination, emptyList(), sim.points, 0.0, sim.totalM, false))

    override fun addWaypoint(place: RoutePlace): Boolean {
        if (waypoints.value.any { it.id == place.id }) return false
        waypoints.value = waypoints.value + place
        return true
    }

    override fun removeWaypoint(id: String) {
        waypoints.value = waypoints.value.filterNot { it.id == id }
    }
}

/** STT 자리. 마이크 대신 데모 패널에서 고른 샘플 문장을 1.2초 뒤 인식 결과로 흘려보낸다. */
class SampleSttProvider(private val scope: CoroutineScope) : SttProvider {
    private val _listening = MutableStateFlow(false)
    override val listening: StateFlow<Boolean> = _listening
    private val _transcripts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val transcripts: SharedFlow<String> = _transcripts

    override fun startListening() { _listening.value = true }
    override fun stopListening() { _listening.value = false }

    fun simulateUtterance(text: String, delayMs: Long = 1200) {
        scope.launch {
            _listening.value = true
            delay(delayMs)
            _listening.value = false
            _transcripts.emit(text)
        }
    }
}
