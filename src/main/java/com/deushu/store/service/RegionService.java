package com.deushu.store.service;

import org.springframework.stereotype.Service;

import com.deushu.store.dto.RegionLocation;

/**
 * region 문자열(쿼리스트링 값)을 위도/경도로 변환해주는 서비스
 */
@Service
public class RegionService {

    /**
     * region 파라미터(예: "chiyoda", "toshin")를 받아서
     * 해당하는 지역의 중심 좌표를 반환한다.
     *
     * 실제 프로젝트에서는 DB에서 읽어오거나, 별도 설정 파일에서 읽어와도 된다.
     */
    public RegionLocation findLocationByRegionCode(String region) {

        if (region == null || region.isBlank()) {
            // region이 없으면 기본값(예: 도쿄역 근처) 반환
            return getDefaultLocation();
        }

        // 소문자로 통일해서 비교 (안전하게)
        String code = region.toLowerCase();

        switch (code) {
            // ============================
            // ① 큰 권역 단위 (예: 都心 / 城南 등)
            // ============================
            case "toshin":   // 都心: 千代田・中央・港
                return new RegionLocation(35.6764, 139.7650, "都心");

            case "jonan":    // 城南: 品川・目黒・大田・世田谷
                return new RegionLocation(35.6093, 139.7077, "城南");

            case "josei":    // 城西: 新宿・渋谷・中野・杉並・練馬
                return new RegionLocation(35.6880, 139.6580, "城西");

            case "johoku":   // 城北: 文京・豊島・北・板橋・足立
                return new RegionLocation(35.7390, 139.7300, "城北");

            case "joto":     // 城東: 台東・墨田・江東・葛飾・江戸川
                return new RegionLocation(35.7110, 139.8300, "城東");

            // ============================
            // ② 구 단위 (각 링크에 있는 코드들)
            // ============================
            // 都心
            case "chiyoda":   // 千代田区
                return new RegionLocation(35.6938, 139.7530, "千代田区");
            case "chuo":      // 中央区
                return new RegionLocation(35.6701, 139.7720, "中央区");
            case "minato":    // 港区
                return new RegionLocation(35.6581, 139.7516, "港区");

            // 城南
            case "shinagawa": // 品川区
                return new RegionLocation(35.6092, 139.7300, "品川区");
            case "meguro":    // 目黒区
                return new RegionLocation(35.6410, 139.6982, "目黒区");
            case "ota":       // 大田区
                return new RegionLocation(35.5614, 139.7160, "大田区");
            case "setagaya":  // 世田谷区
                return new RegionLocation(35.6467, 139.6533, "世田谷区");

            // 城西
            case "shinjuku":  // 新宿区
                return new RegionLocation(35.6938, 139.7034, "新宿区");
            case "shibuya":   // 渋谷区
                return new RegionLocation(35.6618, 139.7041, "渋谷区");
            case "nakano":    // 中野区
                return new RegionLocation(35.7074, 139.6638, "中野区");
            case "suginami":  // 杉並区
                return new RegionLocation(35.6995, 139.6368, "杉並区");
            case "nerima":    // 練馬区
                return new RegionLocation(35.7356, 139.6522, "練馬区");

            // 城北
            case "bunkyo":    // 文京区
                return new RegionLocation(35.7081, 139.7530, "文京区");
            case "toshima":   // 豊島区
                return new RegionLocation(35.7289, 139.7101, "豊島区");
            case "kita":      // 北区
                return new RegionLocation(35.7528, 139.7336, "北区");
            case "itabashi":  // 板橋区
                return new RegionLocation(35.7512, 139.7101, "板橋区");
            case "adachi":    // 足立区
                return new RegionLocation(35.7754, 139.8044, "足立区");

            // 城東
            case "taito":     // 台東区
                return new RegionLocation(35.7121, 139.7887, "台東区");
            case "sumida":    // 墨田区
                return new RegionLocation(35.7107, 139.8015, "墨田区");
            case "koto":      // 江東区
                return new RegionLocation(35.6720, 139.8174, "江東区");
            case "katsushika":// 葛飾区
                return new RegionLocation(35.7433, 139.8473, "葛飾区");
            case "edogawa":   // 江戸川区
                return new RegionLocation(35.7061, 139.8683, "江戸川区");

            // ============================
            // ③ 매칭 안 될 경우 기본값
            // ============================
            default:
                return getDefaultLocation();
        }
    }

    /**
     * region을 못 맞췄을 때 사용할 기본 위치.
     * 여기서는 도쿄역 근처를 기본값으로 사용.
     */
    public RegionLocation getDefaultLocation() {
        return new RegionLocation(35.6812, 139.7671, "東京駅周辺");
    }
}