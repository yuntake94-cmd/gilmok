package com.zainoha.gilmok.content

import com.zainoha.gilmok.sdk.GeoPoint
import com.zainoha.gilmok.sdk.RoutePlace

/** 화면에 그릴 플레이스홀더 일러스트 종류 (Canvas 로 직접 그림, 사진 아님). */
enum class ArtKind { BEACH, SUSPENSION_BRIDGE, ISLANDS, TUNNEL, CABLE_BRIDGE, CASTLE, GARDEN, REST, SHOP, PARKING }

/**
 * 길목 한 곳.
 * - anchor: 경로 위에서 해설을 시작하는 기준점 (경로선의 꼭짓점이어야 함)
 * - viewPoint: 실제 대상의 위치 (방향 화살표·"왼쪽 10시 방향" 계산에 사용)
 * - summary: 주행 중 15초 요약 (TTS). "{dir}" 은 실행 시 상대 방향으로 치환
 * - detail: 정차 중 상세 (TTS 는 이어서 읽음)
 * 모든 수치는 content/sources.md 의 출처를 따른다.
 */
data class Landmark(
    val id: String,
    val name: String,
    val subtitle: String,
    val anchor: GeoPoint,
    val viewPoint: GeoPoint,
    val art: ArtKind,
    val summary: String,
    val detail: List<String>,
    val sampleQuestion: String,
    val sampleAnswer: String,
    val sourceNote: String,
    val triggerBeforeM: Double = 700.0,
    val keepAfterM: Double = 300.0,
)

data class RestStop(
    val id: String,
    val name: String,
    val anchor: GeoPoint,
    val art: ArtKind,
    val tagline: String,
    val note: String,
    val detourMin: Int,
    val colorIndex: Int, // 0 민트, 1 초록, 2 주황
)

object RouteData {

    val destination = RoutePlace("oedo", "거제 외도 (구조라항 선착장)", GeoPoint(34.7960, 128.6730))

    /** 부산 해운대 → 광안대교 → 부산항대교 → 을숙도 → 가덕도 → 거가대로 → 장목 → 구조라항. 대략적인 경로선. */
    val routePoints: List<GeoPoint> = listOf(
        GeoPoint(35.1587, 129.1604), // 해운대 해수욕장 (출발)
        GeoPoint(35.1630, 129.1480),
        GeoPoint(35.1665, 129.1370), // 광안대교 북단 (센텀)
        GeoPoint(35.1560, 129.1300),
        GeoPoint(35.1470, 129.1240), // 광안대교 현수교 구간
        GeoPoint(35.1380, 129.1180),
        GeoPoint(35.1300, 129.1150), // 광안대교 남단 · 용호동 (오륙도 조망)
        GeoPoint(35.1150, 129.1000),
        GeoPoint(35.0980, 129.0700), // 부산항대교
        GeoPoint(35.0900, 129.0500),
        GeoPoint(35.0870, 129.0250), // 남항대교
        GeoPoint(35.0760, 129.0100),
        GeoPoint(35.0940, 128.9500), // 을숙도대교
        GeoPoint(35.0850, 128.8700),
        GeoPoint(35.0750, 128.8400), // 가덕대교
        GeoPoint(35.0500, 128.8350),
        GeoPoint(35.0130, 128.8190), // 가덕해양파크 휴게소
        GeoPoint(35.0010, 128.8130), // 가덕해저터널 입구
        GeoPoint(34.9930, 128.7720), // 대죽도
        GeoPoint(34.9940, 128.7480), // 거가대교 사장교 (중죽도~저도)
        GeoPoint(34.9950, 128.7200), // 장목 유호
        GeoPoint(34.9900, 128.7050), // 장목 · 매미성 조망
        GeoPoint(34.9700, 128.6950), // 장목 지역 상점
        GeoPoint(34.9500, 128.6900),
        GeoPoint(34.9100, 128.6500), // 연초
        GeoPoint(34.8900, 128.6900), // 옥포
        GeoPoint(34.8600, 128.7200), // 장승포
        GeoPoint(34.8300, 128.7100), // 지세포
        GeoPoint(34.8050, 128.6900), // 와현
        GeoPoint(34.7990, 128.6760), // 구조라 해변 주차장
        GeoPoint(34.7960, 128.6730), // 구조라항 (외도 유람선 선착장, 도착)
    )

    val landmarks: List<Landmark> = listOf(
        Landmark(
            id = "haeundae", name = "해운대 해수욕장", subtitle = "출발지 · 백사장 약 1.5km",
            anchor = GeoPoint(35.1587, 129.1604), viewPoint = GeoPoint(35.1587, 129.1604), art = ArtKind.BEACH,
            summary = "해운대 해수욕장에서 출발합니다. 백사장 길이 약 1.5킬로미터, 여름마다 천만 명이 찾는 해변이에요. 길목이 창밖 이야기를 들려드릴게요.",
            detail = listOf(
                "해운대 백사장은 길이 약 1.5km, 복원 사업 뒤 폭이 약 70~90m 로 넓어졌습니다.",
                "2015년 전후 복원 공사로 40m 까지 좁아졌던 백사장 폭을 두 배로 되돌렸습니다.",
                "서쪽 끝 동백섬에 서면 광안대교와 오륙도가 한눈에 들어옵니다.",
            ),
            sampleQuestion = "여기 해변 길이가 얼마나 돼?",
            sampleAnswer = "해운대 백사장은 약 1.5킬로미터예요. 복원 공사 뒤로 폭도 70에서 90미터로 넓어졌어요.",
            sourceNote = "출처: 텔트립 2026 해운대 개장 기사 · 서울신문 2015-05-23",
            triggerBeforeM = 100.0, keepAfterM = 320.0,
        ),
        Landmark(
            id = "gwangan", name = "광안대교", subtitle = "총길이 7.42km · 현수교 구간 900m · 2003년 개통",
            anchor = GeoPoint(35.1470, 129.1240), viewPoint = GeoPoint(35.1470, 129.1240), art = ArtKind.SUSPENSION_BRIDGE,
            summary = "광안대교를 건넙니다. 총길이 7.42킬로미터, 가운데 900미터는 현수교 구간이에요. 2003년 1월에 열렸습니다.",
            detail = listOf(
                "1994년 12월 착공, 2002년 12월 준공, 2003년 1월 6일 개통했습니다.",
                "총길이 7.42km 가운데 현수교 구간이 900m 입니다.",
                "2024년 경관조명을 바꾸면서 등기구가 7,011등에서 13,465등으로 늘고 간격은 1m 에서 0.5m 로 촘촘해졌습니다.",
            ),
            sampleQuestion = "이 다리 길이가 얼마야?",
            sampleAnswer = "광안대교는 총길이 7.42킬로미터이고, 지금 지나는 가운데 현수교 구간이 900미터예요.",
            sourceNote = "출처: 부산광역시 뉴스(2024 경관조명) · 뉴스서울 · 경향신문 2013-01-08",
            triggerBeforeM = 900.0, keepAfterM = 500.0,
        ),
        Landmark(
            id = "oryukdo", name = "오륙도", subtitle = "명승 제24호 · 물때 따라 다섯 섬 또는 여섯 섬",
            anchor = GeoPoint(35.1300, 129.1150), viewPoint = GeoPoint(35.0937, 129.1250), art = ArtKind.ISLANDS,
            summary = "{dir}으로 오륙도가 보입니다. 밀물 때는 여섯 개, 썰물 때는 다섯 개로 보여서 오륙도라 부릅니다.",
            detail = listOf(
                "선착장에서 가장 가까운 우삭도는 썰물 때 한 섬이지만 밀물 때 방패섬과 솔섬으로 갈라집니다. 이 조화에서 오륙도라는 이름이 나왔습니다.",
                "1972년 부산시 기념물 제22호로 지정됐고 2007년 명승 제24호로 승격됐습니다.",
                "가장 바깥 등대섬은 위가 평평해 밭섬이라 불리다 등대가 세워진 뒤 이름이 바뀌었습니다.",
            ),
            sampleQuestion = "저 멀리 보이는 섬 이름이 뭐야?",
            sampleAnswer = "오륙도예요. 밀물 때 우삭도가 둘로 갈라져 여섯 개, 썰물 때 다섯 개로 보여서 붙은 이름이고, 지금은 {dir}에 있어요.",
            sourceNote = "출처: 한국관광공사 대한민국구석구석 · 한국교육신문",
        ),
        Landmark(
            id = "tunnel", name = "가덕해저터널", subtitle = "길이 3.7km · 최대 수심 48m 침매터널 · 2010년 개통",
            anchor = GeoPoint(35.0010, 128.8130), viewPoint = GeoPoint(34.9970, 128.7930), art = ArtKind.TUNNEL,
            summary = "이제 바다 밑으로 들어갑니다. 가덕해저터널은 길이 3.7킬로미터, 최대 수심 48미터의 침매터널로, 콘크리트 함체 18개를 가라앉혀 이었어요.",
            detail = listOf(
                "육상에서 만든 길이 180m·높이 9.97m·너비 26.5m 콘크리트 함체 18개를 바다 밑에 가라앉혀 연결했습니다.",
                "최대 수심 48m 구간에 놓여 침매터널 가운데 세계에서 가장 깊은 것으로 알려졌습니다.",
                "2010년 12월 13일 개통식 뒤 14일부터 차량이 다니기 시작했고, 부산–거제 통행시간이 130분에서 50분으로 줄었습니다.",
            ),
            sampleQuestion = "지금 바다 밑 몇 미터야?",
            sampleAnswer = "가장 깊은 곳은 바다 밑 48미터예요. 길이 3.7킬로미터 침매터널이라 금방 다시 바다 위로 나가요.",
            sourceNote = "출처: 서울신문 2010-09-08 · 2010-12-13 · 뉴스1 낙동포럼",
            triggerBeforeM = 800.0, keepAfterM = 800.0,
        ),
        Landmark(
            id = "geoga", name = "거가대교", subtitle = "사장교 구간 4.5km · 거가대로 8.2km · 2010년 개통",
            anchor = GeoPoint(34.9940, 128.7480), viewPoint = GeoPoint(34.9940, 128.7480), art = ArtKind.CABLE_BRIDGE,
            summary = "거가대교 사장교 구간이에요. 중죽도와 저도를 건너 거제 장목으로 이어지는 4.5킬로미터, 해저터널과 합쳐 거가대로 8.2킬로미터입니다.",
            detail = listOf(
                "거가대로는 가덕해저터널 3.7km 와 사장교 구간 거가대교 4.5km 를 합친 8.2km 입니다.",
                "개통으로 부산–거제 거리는 140km 에서 60km 로, 통행시간은 50분 안팎으로 줄었습니다.",
                "사장교 교각 디자인과 침매공법 해저터널이 남해 풍경과 어우러져 건설 당시부터 주목받았습니다.",
            ),
            sampleQuestion = "이 다리 길이가 얼마야?",
            sampleAnswer = "거가대교 사장교 구간은 4.5킬로미터예요. 방금 지난 해저터널까지 합치면 거가대로 전체가 8.2킬로미터예요.",
            sourceNote = "출처: 경북매일 2014-07-21 · 경남신문 · KNN",
            triggerBeforeM = 900.0, keepAfterM = 600.0,
        ),
        Landmark(
            id = "maemi", name = "매미성", subtitle = "장목면 복항마을 · 거제 9경 · 무료",
            anchor = GeoPoint(34.9900, 128.7050), viewPoint = GeoPoint(34.9897, 128.7029), art = ArtKind.CASTLE,
            summary = "{dir} 바닷가에 매미성이 있어요. 2003년 태풍 매미로 농지를 잃은 주민 한 분이 혼자 쌓아 올린 돌 성벽이에요.",
            detail = listOf(
                "2003년 태풍 매미로 농지가 유실된 뒤 한 주민이 네모난 돌을 하나씩 쌓고 시멘트로 메워 만든 성벽입니다. 지금도 완성되지 않아 축성이 이어집니다.",
                "2023년 한 해 100만 명 넘게 다녀가며 거제 9경에 들었습니다.",
                "성벽의 아치형 창 너머로 거가대교가 액자처럼 들어옵니다.",
            ),
            sampleQuestion = "저기 성 같은 건 뭐야?",
            sampleAnswer = "매미성이에요. 태풍 매미 뒤에 주민 한 분이 20년 가까이 혼자 쌓은 돌 성벽이고, 지금 {dir}에 있어요.",
            sourceNote = "출처: 뉴시스 2018-09-02 · KPI뉴스 · 텔트립",
        ),
        Landmark(
            id = "oedo", name = "외도 보타니아", subtitle = "1995년 개장 해상 식물원 · 누적 방문 2,000만 명",
            anchor = GeoPoint(34.7960, 128.6730), viewPoint = GeoPoint(34.7760, 128.7050), art = ArtKind.GARDEN,
            summary = "구조라항에 도착했습니다. 여기서 배를 타면 외도 보타니아예요. 1995년 문을 연 해상 식물원으로, 누적 방문객이 2천만 명을 넘었습니다.",
            detail = listOf(
                "1969년 한 부부가 버려진 섬을 사들여 1976년부터 본격적으로 나무와 꽃을 심기 시작했습니다.",
                "1995년 외도해상공원으로 문을 열었고, 지금까지 누적 방문객이 2,000만 명을 넘었습니다.",
                "차는 선착장에 두고 유람선으로 들어가는 섬이라 길목 해설은 여기서 도보 코스로 이어집니다.",
            ),
            sampleQuestion = "여기서 외도까지 얼마나 남았어?",
            sampleAnswer = "외도는 {dir}, 배로 들어가는 섬이에요.",
            sourceNote = "출처: 브라보마이라이프 · 전북일보 2023-09-07 · 텔트립",
            triggerBeforeM = 900.0, keepAfterM = 100.0,
        ),
    )

    val restStops: List<RestStop> = listOf(
        RestStop("park_gadeok", "가덕해양파크 휴게소", GeoPoint(35.0130, 128.8190), ArtKind.REST,
            "거가대교 홍보전시관 · 2층 바다 전망", "화장실 · 거가대로 요금소 옆", 3, 0),
        RestStop("maemi_stop", "매미성 (장목)", GeoPoint(34.9900, 128.7050), ArtKind.CASTLE,
            "거가대교 조망 성벽 · 무료", "주차 후 도보 5분", 8, 1),
        RestStop("jangmok_shop", "장목 지역 상점", GeoPoint(34.9700, 128.6950), ArtKind.SHOP,
            "지역 제휴 쿠폰 10% (데모 예시)", "제휴 모델 시연용 가상 카드", 12, 2),
        RestStop("gujora_parking", "구조라 해변 주차장", GeoPoint(34.7990, 128.6760), ArtKind.PARKING,
            "도착지 · 해변 산책", "외도 유람선 선착장 도보권", 0, 0),
    )

    val sampleQuestions: List<String> = listOf(
        "저 멀리 보이는 섬 이름이 뭐야?",
        "이 다리 길이가 얼마야?",
        "지금 바다 밑 몇 미터야?",
        "저기 성 같은 건 뭐야?",
        "여기서 외도까지 얼마나 남았어?",
        "쉬어 갈 데 있어?",
    )

    /** 연속 주행 45분(시뮬레이션 시간)이면 쉼표 제안. */
    const val REST_THRESHOLD_SEC = 45L * 60L
}
