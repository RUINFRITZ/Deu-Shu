package com.deushu.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지도 보이는 영역(Bounding Box) 좌표 DTO
 * Leaflet map.getBounds() 결과를 서버로 전달할 때 사용.
 * 지도 이동(moveend) 이벤트 시 현재 뷰포트 범위를 표현한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bbox {

    /** 남서쪽(SW) 위도 - 보이는 영역의 최소 위도 */
    private Double minLat;

    /** 북동쪽(NE) 위도 - 보이는 영역의 최대 위도 */
    private Double maxLat;

    /** 남서쪽(SW) 경도 - 보이는 영역의 최소 경도 */
    private Double minLng;

    /** 북동쪽(NE) 경도 - 보이는 영역의 최대 경도 */
    private Double maxLng;

    /**
     * 네 모서리 좌표가 모두 유효한지 검증.
     * API 파라미터 수신 시 null 체크에 사용.
     */
    public boolean isValid() {
        return minLat != null && maxLat != null
            && minLng != null && maxLng != null;
    }
}