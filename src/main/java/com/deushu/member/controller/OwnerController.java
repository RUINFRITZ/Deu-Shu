package com.deushu.member.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.deushu.common.response.ApiResponse;
import com.deushu.member.domain.MemberEntity;
import com.deushu.member.mapper.MemberRepository;
import com.deushu.order.domain.ItemEntity;
import com.deushu.order.dto.PickupVerifyResponse;
import com.deushu.order.dto.SalesSummaryDto;
import com.deushu.order.service.OrderQrService;
import com.deushu.store.domain.StoreEntity;
import com.deushu.store.dto.StoreImageDto;
import com.deushu.store.mapper.ItemRepository;
import com.deushu.store.mapper.SalesRepository;
import com.deushu.store.mapper.StoreMapper;
import com.deushu.store.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 사업자(ROLE_OWNER) 전용 REST API 컨트롤러 v2
 *
 * 변경 사항:
 *  - POST  /api/owner/upload          : S3 파일 업로드 (multipart)
 *  - GET   /api/owner/business/{num}  : lat/lng 도 반환하도록 수정
 *  - PATCH /api/owner/store/address   : 주소·위경도 단독 upsert
 *  - StoreRequest 에 businessNumber 필드 추가
 *  - registerStore/updateStore 에 businessNumber 처리
 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {
	
    private final OrderQrService orderQrService;
    private final MemberRepository memberRepository;
    private final StoreMapper      storeMapper;
    private final ItemRepository   itemRepository;
    private final SalesRepository  salesRepository;
    private final S3Service        s3Service;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;



    // =====================================================================
    // 요청 DTO
    // =====================================================================

    @Data
    public static class StoreRequest {
        private String name;
        private String category;
        private String address;
        private Double lat;
        private Double lng;
        private String openTime;
        private String closeTime;
        private String thumbnailUrl;
        private String info;
        private String businessNumber;  // ① 사업자등록번호 추가
    }

    @Data
    public static class StoreImageRequest {
        private String imageUrl;
        private int sortOrder;
    }

    /** ⑤ 주소·위경도 단독 업데이트 요청 DTO */
    @Data
    public static class AddressUpdateRequest {
        private String address;
        private Double lat;
        private Double lng;
    }

    @Data
    public static class ItemRequest {
        private String name;
        private int originalPrice;
        private int discountPrice;
        private int discountRate;
        private int stock;
        private String expireAt;
        private String thumbnailUrl;
    }

    // =====================================================================
    // 공통 유틸
    // =====================================================================

    private MemberEntity getOwner(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) return null;
        MemberEntity member = memberRepository.findById(memberId);
        if (member == null) return null;
        if (!"ROLE_OWNER".equals(member.getRole())
                && !"ROLE_ADMIN".equals(member.getRole())) return null;
        return member;
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "権限がありません。オーナーアカウントでログインしてください");
        return ResponseEntity.status(403).body(result);
    }

    // =====================================================================
    // ③⑦ S3 파일 업로드
    // POST /api/owner/upload
    // Content-Type: multipart/form-data
    // 반환: { success: true, url: "https://bucket.s3.region.amazonaws.com/..." }
    // =====================================================================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestPart("file") MultipartFile file,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        if (getOwner(session) == null) return unauthorized();

        try {
            String original = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "file";
            String ext = original.contains(".")
                    ? original.substring(original.lastIndexOf('.')) : "";
            String key = "stores/" + UUID.randomUUID() + ext;

            // S3Service 에 업로드 위임 (ACL 없음 — 버킷 정책으로 공개 제어)
            s3Service.upload(file, key);

            // 공개 URL 생성
            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

            result.put("success", true);
            result.put("url", url);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "アップロードに失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    // =====================================================================
    // ①⑤ 사업자번호로 주소 조회 + OpenStreetMap Nominatim geocoding
    // GET /api/owner/business/{businessNumber}
    // 흐름: mock_business_registry → 주소 취득 → Nominatim API → lat/lng 반환
    // =====================================================================

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping("/business/{businessNumber}")
    public ResponseEntity<Map<String, Object>> getBusinessInfo(
            @PathVariable("businessNumber") String businessNumber,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        if (getOwner(session) == null) return unauthorized();

        // 1) mock_business_registry 에서 주소 조회
        StoreEntity bizInfo = storeMapper.findBusinessByNumber(businessNumber);
        if (bizInfo == null) {
            result.put("success", false);
            result.put("message", "事業者情報が見つかりません");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.put("address", bizInfo.getAddress());
        result.put("storeName", bizInfo.getName());

        // 2) 주소 → 国土地理院 AddressSearch API 로 lat/lng 취득
        try {
            String encodedAddress = URLEncoder.encode(bizInfo.getAddress(), StandardCharsets.UTF_8);

            String gsiUrl = "https://msearch.gsi.go.jp/address-search/AddressSearch"
                    + "?q=" + encodedAddress;

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(gsiUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> httpRes =
                    HTTP_CLIENT.send(httpReq, HttpResponse.BodyHandlers.ofString());

            if (httpRes.statusCode() != 200) {
                throw new IllegalStateException("GSI API status: " + httpRes.statusCode());
            }

            JsonNode root = OBJECT_MAPPER.readTree(httpRes.body());

            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                JsonNode coordinates = first.path("geometry").path("coordinates");

                // GSI GeoJSON: [lng, lat]
                if (coordinates.isArray() && coordinates.size() >= 2) {
                    double lng = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();

                    result.put("lat", lat);
                    result.put("lng", lng);
                } else {
                    result.put("lat", null);
                    result.put("lng", null);
                    result.put("geocodeWarning",
                            "住所検索結果に座標情報が含まれていませんでした。");
                }
            } else {
                result.put("lat", null);
                result.put("lng", null);
                result.put("geocodeWarning",
                        "住所から位置情報を取得できませんでした。地図上の位置は後で修正できます。");
            }

        } catch (Exception e) {
            result.put("lat", null);
            result.put("lng", null);
            result.put("geocodeWarning",
                    "位置情報の取得中にエラーが発生しました: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
    // =====================================================================
    // 가게 기본 정보 조회 / 등록 / 수정
    // =====================================================================

    @GetMapping("/store")
    public ResponseEntity<Map<String, Object>> getStore(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        result.put("success", true);
        result.put("store", store);
        if (store != null) {
            List<StoreImageDto> images = storeMapper.findImagesByStoreId(store.getId());
            result.put("images", images);
        }
        return ResponseEntity.ok(result);
    }

 // POST /api/owner/store 하나만 남기고, PUT은 삭제
    @PostMapping("/store")
    public ResponseEntity<Map<String, Object>> saveStore(
            @RequestBody StoreRequest req,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = new StoreEntity();
        store.setOwnerId(owner.getId());
        store.setName(req.getName());
        store.setCategory(req.getCategory());
        store.setAddress(req.getAddress());
        store.setLat(req.getLat() != null ? new BigDecimal(req.getLat()) : BigDecimal.ZERO);
        store.setLng(req.getLng() != null ? new BigDecimal(req.getLng()) : BigDecimal.ZERO);
        store.setOpenTime(LocalTime.parse(req.getOpenTime()));
        store.setCloseTime(LocalTime.parse(req.getCloseTime()));
        store.setThumbnailUrl(req.getThumbnailUrl());
        store.setInfo(req.getInfo());
        store.setBusinessNumber(req.getBusinessNumber() != null ? req.getBusinessNumber() : "N/A");

        storeMapper.upsertStore(store);  // INSERT or UPDATE 자동 처리

        result.put("success", true);
        result.put("message", "店舗情報が保存されました");
        result.put("storeId", store.getId());
        return ResponseEntity.ok(result);
    }
    // PUT /api/owner/store 메서드 전체 삭제

    // =====================================================================
    // ⑤ 주소·위경도 단독 upsert
    // PATCH /api/owner/store/address
    // btnRefreshAddress 클릭 시 가게가 이미 등록된 경우 호출
    // =====================================================================

    @PatchMapping("/store/address")
    public ResponseEntity<Map<String, Object>> updateAddress(
            @RequestBody AddressUpdateRequest req,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", false);
            result.put("message", "店舗が登録されていません");
            return ResponseEntity.badRequest().body(result);
        }

        store.setAddress(req.getAddress());
        if (req.getLat() != null) store.setLat(new BigDecimal(req.getLat()));
        if (req.getLng() != null) store.setLng(new BigDecimal(req.getLng()));

        storeMapper.updateAddress(store);   // 주소·위경도 전용 쿼리 (다른 컬럼 보호)

        result.put("success", true);
        result.put("message", "住所・位置情報を更新しました");
        return ResponseEntity.ok(result);
    }

    // =====================================================================
    // 가게 추가 사진 관리
    // =====================================================================

    @PostMapping("/store/images")
    public ResponseEntity<Map<String, Object>> addStoreImage(
            @RequestBody StoreImageRequest req,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", false);
            result.put("message", "店舗が登録されていません");
            return ResponseEntity.badRequest().body(result);
        }

        StoreImageDto image = new StoreImageDto();
        image.setStoreId(store.getId());
        image.setImageUrl(req.getImageUrl());
        image.setSortOrder(req.getSortOrder());
        storeMapper.insertImage(image);

        result.put("success", true);
        result.put("message", "写真が追加されました");
        result.put("imageId", image.getId());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/store/images/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteStoreImage(
            @PathVariable("imageId") Long imageId,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", false);
            result.put("message", "店舗が見つかりません");
            return ResponseEntity.badRequest().body(result);
        }

        storeMapper.deleteImage(imageId, store.getId());
        result.put("success", true);
        result.put("message", "写真が削除されました");
        return ResponseEntity.ok(result);
    }

    // =====================================================================
    // 상품 관리 (items)
    // =====================================================================

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> getItems(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", true);
            result.put("items", List.of());
            return ResponseEntity.ok(result);
        }

        List<ItemEntity> items = itemRepository.findByStoreId(store.getId());
        result.put("success", true);
        result.put("items", items);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> registerItem(
            @RequestBody ItemRequest req,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", false);
            result.put("message", "先に店舗を登録してください");
            return ResponseEntity.badRequest().body(result);
        }

        int rate = req.getDiscountRate();
        if (rate == 0 && req.getOriginalPrice() > 0) {
            rate = (int) Math.round(
                    (1.0 - (double) req.getDiscountPrice() / req.getOriginalPrice()) * 100);
        }

        ItemEntity item = new ItemEntity();
        item.setStoreId(store.getId());
        item.setName(req.getName());
        item.setOriginalPrice(req.getOriginalPrice());
        item.setDiscountPrice(req.getDiscountPrice());
        item.setDiscountRate(rate);
        item.setStock(req.getStock());
        item.setExpireAt(LocalDateTime.parse(req.getExpireAt()));
        item.setThumbnailUrl(req.getThumbnailUrl());
        itemRepository.insert(item);

        result.put("success", true);
        result.put("message", "商品が登録されました");
        result.put("itemId", item.getId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable("itemId") Long itemId,
            @RequestBody ItemRequest req,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", false);
            result.put("message", "店舗情報が見つかりません");
            return ResponseEntity.badRequest().body(result);
        }

        int rate = req.getDiscountRate();
        if (rate == 0 && req.getOriginalPrice() > 0) {
            rate = (int) Math.round(
                    (1.0 - (double) req.getDiscountPrice() / req.getOriginalPrice()) * 100);
        }

        ItemEntity item = new ItemEntity();
        item.setId(itemId);
        item.setStoreId(store.getId());
        item.setName(req.getName());
        item.setOriginalPrice(req.getOriginalPrice());
        item.setDiscountPrice(req.getDiscountPrice());
        item.setDiscountRate(rate);
        item.setStock(req.getStock());
        item.setExpireAt(LocalDateTime.parse(req.getExpireAt()));
        item.setThumbnailUrl(req.getThumbnailUrl());
        itemRepository.update(item);

        result.put("success", true);
        result.put("message", "商品情報が更新されました");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            @PathVariable("itemId") Long itemId,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        if (getOwner(session) == null) return unauthorized();
        itemRepository.delete(itemId);
        result.put("success", true);
        result.put("message", "商品が削除されました");
        return ResponseEntity.ok(result);
    }

    // =====================================================================
    // 매출 정산
    // =====================================================================

    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> getSales(HttpSession session) {
        return buildSalesResponse(session);
    }

    @PostMapping("/sales/refresh")
    public ResponseEntity<Map<String, Object>> refreshSales(HttpSession session) {
        return buildSalesResponse(session);
    }

    private ResponseEntity<Map<String, Object>> buildSalesResponse(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        MemberEntity owner = getOwner(session);
        if (owner == null) return unauthorized();

        StoreEntity store = storeMapper.findByOwnerId(owner.getId());
        if (store == null) {
            result.put("success", true);
            result.put("summary", null);
            result.put("daily", List.of());
            return ResponseEntity.ok(result);
        }

        SalesSummaryDto summary = salesRepository.findTotalSummary(store.getId());
        List<SalesSummaryDto> daily = salesRepository.findDailySummary(store.getId(), 30);

        result.put("success", true);
        result.put("summary", summary);
        result.put("daily", daily);
        result.put("refreshedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
    

    // ── 오너: QR 코드(pickupCode)로 주문 조회 ─────────────────────────
    /**
     * GET /api/owner/pickup/{pickupCode}
     * ownerPage.html의 verifyPickup() 에서 호출.
     * 본인 가게 주문인지 storeId로 자동 검증.
     */
    @GetMapping("/pickup/{pickupCode}")
    public ResponseEntity<ApiResponse<PickupVerifyResponse>> getPickupOrder(
            @PathVariable("pickupCode") String pickupCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long ownerId = extractMemberIdFromPrincipal(userDetails);
        if (ownerId == null) return ResponseEntity.status(401).build();

        PickupVerifyResponse result = orderQrService.verifyPickup(pickupCode, ownerId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    // ── 오너: 픽업 완료 처리 ──────────────────────────────────────────
    /**
     * PATCH /api/owner/pickup/{pickupCode}/complete
     * ownerPage.html의 completePickup() 에서 호출.
     * PAYMENT_COMPLETED → PICKUP_COMPLETED 상태 변경.
     */
    @PatchMapping("/pickup/{pickupCode}/complete")
    public ResponseEntity<ApiResponse<String>> completePickup(
            @PathVariable("pickupCode") String pickupCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long ownerId = extractMemberIdFromPrincipal(userDetails);
        if (ownerId == null) return ResponseEntity.status(401).build();

        orderQrService.completePickup(pickupCode, ownerId);
        return ResponseEntity.ok(ApiResponse.onSuccess("ピックアップ完了に更新しました。"));
    }

    private Long extractMemberIdFromPrincipal(UserDetails userDetails) {
        if (userDetails == null) return null;
        String loginId = userDetails.getUsername();
        return memberRepository.findIdByLoginId(loginId);
    }
}