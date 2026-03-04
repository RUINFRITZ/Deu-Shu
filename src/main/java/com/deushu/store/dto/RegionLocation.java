package com.deushu.store.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 특정 지역의 중심 좌표와 표시용 이름을 담는 DTO
 * 메인페이지 nav 버튼 클릭시 전달받은 지역명 파라미터에 service 단에서 위,경도 추가할때 필요.
 */
@Data
@AllArgsConstructor
public class RegionLocation {

    // 지도 중심에 사용할 위도
    private double lat;

    // 지도 중심에 사용할 경도
    private double lng;

    // 화면에 표시할 일본어 지역명 (예: 千代田区, 都心 등)
    private String displayName;
}
