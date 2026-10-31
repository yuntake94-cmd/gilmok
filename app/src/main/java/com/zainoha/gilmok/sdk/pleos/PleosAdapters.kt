package com.zainoha.gilmok.sdk.pleos

import com.zainoha.gilmok.sdk.LocationFix
import com.zainoha.gilmok.sdk.LocationProvider
import com.zainoha.gilmok.sdk.NaviProvider
import com.zainoha.gilmok.sdk.RoutePlace
import com.zainoha.gilmok.sdk.RouteState
import com.zainoha.gilmok.sdk.VehicleProvider
import com.zainoha.gilmok.sdk.VehicleState
import kotlinx.coroutines.flow.StateFlow

/**
 * Pleos Connect SDK 어댑터 자리 (아직 미구현).
 *
 * Pleos Playground 문서(document.pleos.ai, NaviHelper SDK v3.0.5 기준)의 호출 순서는
 *   1) SDK 객체 생성 → 2) initialize() → 3) addListener()/콜백 등록
 *   → 4) 요청 API 호출 → 5) 콜백으로 결과·상태 수신 → 6) removeListener() → release()
 * 이며, 아래 어댑터는 각 콜백을 StateFlow 로 바꿔 길목 엔진에 넘기는 역할만 한다.
 * 의존성 예: ai.pleos.playground:FusedLocation:1.0.4 (문서 예시) — 실제 버전·패키지명은 Playground 에서 확인.
 *
 * 교체 방법: GilmokViewModel 에서 Sim*Provider 대신 이 클래스들을 생성하면 끝 (README 참고).
 */
class PleosVehicleProvider : VehicleProvider {
    // TODO: Vehicle SDK 의 속도·기어 신호 콜백을 받아 VehicleState 로 변환
    override val state: StateFlow<VehicleState>
        get() = TODO("Pleos Vehicle SDK 연결 후 구현")
}

class PleosLocationProvider : LocationProvider {
    // TODO: FusedLocation SDK initialize() → 위치 콜백 등록, 위도·경도·bearing 을 LocationFix 로 변환
    override val fix: StateFlow<LocationFix>
        get() = TODO("Pleos FusedLocation SDK 연결 후 구현")
}

class PleosNaviProvider : NaviProvider {
    // TODO: NaviHelper initialize() → addListener(NaviHelperEventListener) 로 경로·주행 상태 이벤트 수신
    override val route: StateFlow<RouteState>
        get() = TODO("Pleos NaviHelper SDK 연결 후 구현")

    // TODO: NaviHelper 경유지 추가 API 호출 결과를 Boolean 으로 반환
    override fun addWaypoint(place: RoutePlace): Boolean = TODO("Pleos NaviHelper SDK 연결 후 구현")
    override fun removeWaypoint(id: String): Unit = TODO("Pleos NaviHelper SDK 연결 후 구현")
}
