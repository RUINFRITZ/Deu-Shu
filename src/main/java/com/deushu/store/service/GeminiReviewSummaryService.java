package com.deushu.store.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.deushu.store.dto.ReviewForSummaryDto;
import com.deushu.store.dto.ReviewSummaryResponseDto;
import com.deushu.store.mapper.ReviewSummaryMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gemini 1.5 Flash 기반 리뷰 요약 서비스
 *
 * [Map-Reduce 처리 흐름]
 *  1. DB에서 해당 가게의 전체 리뷰 조회
 *  2. Map 단계: 리뷰를 100개 청크로 분할 → 각 청크를 Gemini로 중간 요약
 *  3. Reduce 단계: 중간 요약들을 모아 Gemini로 최종 요약 생성
 *  4. 최종 JSON 파싱 → ReviewSummaryResponseDto 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiReviewSummaryService {

    private final ReviewSummaryMapper reviewSummaryMapper;
    private final Gson gson = new Gson();

    // ── Gemini API 설정 ─────────────────────────────────────────────
    // application.yml: gemini.api.key 값을 주입
    // @Value는 빈 생성 후 주입 → 필드 선언 시 사용 불가, callGeminiApi() 내에서 동적 조합
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    // application.yml 예시: gemini.model=gemini-1.5-flash:generateContent
    @Value("${gemini.model}")
    private String geminiModel;

    // 청크 크기: 100개 단위로 분할 (Map 단계)
    private static final int CHUNK_SIZE = 100;

    // WebClient는 매 요청 시 생성 (간단한 사용 빈도 - 빈 설정 불필요)
    private final WebClient webClient = WebClient.builder().build();

    // ── 진입점 ───────────────────────────────────────────────────────

    /**
     * 특정 가게의 리뷰를 Map-Reduce 방식으로 요약하여 반환
     *
     * @param storeId 가게 ID
     * @return 최종 요약 DTO (리뷰 없으면 hasReviews=false)
     */
    public ReviewSummaryResponseDto summarize(Long storeId) {

        // 1. 전체 리뷰 조회
        List<ReviewForSummaryDto> reviews =
                reviewSummaryMapper.findAllReviewsByStoreId(storeId);

        // 2. 리뷰 없는 경우 조기 반환
        if (reviews == null || reviews.isEmpty()) {
            log.info("[AI 요약] storeId={} 리뷰 없음 → 빈 응답 반환", storeId);
            return ReviewSummaryResponseDto.builder()
                    .hasReviews(false)
                    .build();
        }

        log.info("[AI 요약] storeId={} 총 {}개 리뷰 처리 시작", storeId, reviews.size());

        // 3. Map 단계: 100개 청크로 분할 → 각 청크 중간 요약
        List<List<ReviewForSummaryDto>> chunks = partition(reviews, CHUNK_SIZE);
        List<String> intermediateSummaries = chunks.stream()
                .map(this::summarizeChunk)
                .collect(Collectors.toList());

        log.info("[AI 요약] Map 완료: {}개 중간 요약 생성", intermediateSummaries.size());

        // 4. Reduce 단계: 중간 요약들을 모아 최종 요약 생성
        //    청크가 1개면 Reduce 불필요 → 그대로 사용
        String finalJson = (intermediateSummaries.size() == 1)
                ? intermediateSummaries.get(0)
                : reduceSummaries(intermediateSummaries);

        log.info("[AI 요약] Reduce 완료: 최종 JSON 생성");

        // 5. JSON → DTO 변환 후 반환
        return parseToDto(finalJson);
    }

    // ── Map 단계: 청크 1개 중간 요약 ────────────────────────────────

    /**
     * 리뷰 100개 이하 한 청크를 Gemini API로 중간 요약
     * 프론트가 일본어 UI이므로 일본어로 출력하도록 프롬프트 지시
     *
     * @param chunk 리뷰 목록 (최대 100개)
     * @return Gemini가 반환한 중간 요약 JSON 문자열
     */
    private String summarizeChunk(List<ReviewForSummaryDto> chunk) {
        // 리뷰 배열을 JSON 문자열로 직렬화
        String reviewsJson = gson.toJson(chunk);

        // Map 단계 프롬프트: 일본어 출력, JSON 형식 엄수
        String prompt = """
                あなたはレビュー分析の専門家AIです。
                提供されたレビューリスト 内容を分析し、要点をまとめてください。

                【重要なルール】
                - 返答はJSON形式のみ。前置き・説明文・マークダウン(```)は 絶対に含めないでください。
                - すべての値は必ず日本語で作成してください。
                - 以下のJSON構造を厳守してください：
                {
                  "summary": "全体のレビューを網羅する最終要約 (2~3文程度)",
                  "keywords": ["最も多く言及されたキーワード1", "キーワード2", "キーワード3"]
                }

                レビューリスト：
                """ + reviewsJson;
        return callGeminiApi(prompt);
    }

    // ── Reduce 단계: 중간 요약들 → 최종 요약 ─────────────────────────

    /**
     * 여러 청크의 중간 요약을 하나의 최종 요약으로 통합
     *
     * @param summaries Map 단계에서 생성된 중간 요약 JSON 문자열 목록
     * @return Gemini가 반환한 최종 요약 JSON 문자열
     */
    private String reduceSummaries(List<String> summaries) {
        // 중간 요약들을 JSON 배열로 조합
        String summariesJson = "[" + String.join(",", summaries) + "]";

        // Reduce 단계 프롬프트: 일본어 출력, JSON 형식 엄수
        String prompt = """
                あなたはレビュー分析の専門家AIです。
        		複数の要約データを統合し、最終的に整理してください。
                【重要なルール】
                - 返答はJSON形式のみ。
                - すべての値は必ず日本語で作成してください。
                - 以下のJSON構造を厳守してください：
                {
                  "summary": "全体のレビューを網羅する最終要約 (3文程度)",
                  "keywords": ["最も多く言及されたキーワード1", "キーワード2", "キーワード3"]
                }

                中間要約 リスト：
                """ + summariesJson;

        return callGeminiApi(prompt);
    }

    // ── Gemini API 실제 호출 ─────────────────────────────────────────

    /**
     * Gemini 1.5 Flash API에 프롬프트 전송 후 텍스트 응답 반환
     * responseMimeType: "application/json" 으로 JSON 응답 강제
     *
     * @param prompt 전송할 프롬프트 문자열
     * @return Gemini가 반환한 텍스트 (JSON 형식)
     */
    private String callGeminiApi(String prompt) {
    	JsonObject requestBody = new JsonObject();

        // 1. contents 구성
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();
        // role은 생략 가능하지만 명시하는 것이 안전합니다.
        contentObj.addProperty("role", "user"); 
        
        JsonArray partsArray = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        
        partsArray.add(textPart);
        contentObj.add("parts", partsArray);
        contentsArray.add(contentObj);
        requestBody.add("contents", contentsArray);

        // 2. generationConfig 구성 (400 에러 해결의 핵심)
        JsonObject generationConfig = new JsonObject();
        // 필드명이 responseMimeType이 아니라 response_mime_type인지 확인
        generationConfig.addProperty("response_mime_type", "application/json");
        generationConfig.addProperty("temperature", 0.1); 
        
        requestBody.add("generationConfig", generationConfig);
        try {
            // URI 생성 (이전 단계에서 수정한 정상 경로)
            java.net.URI finalUri = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path("/v1beta/models/{model}:generateContent")
                    .queryParam("key", apiKey)
                    .buildAndExpand(geminiModel)
                    .toUri();

            String rawResponse = webClient.post()
                    .uri(finalUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(gson.toJson(requestBody)) // toString() 대신 gson.toJson 권장
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractTextFromGeminiResponse(rawResponse);

        } catch (Exception e) {
            // 에러 로그에 응답 바디를 찍어보면 더 정확한 원인을 알 수 있습니다.
            log.error("[AI 요약] Gemini API 호출 실패. 원인: {}", e.getMessage());
            return "{\"summary\":\"요약 실패\",\"keywords\":[]}";
        }
    }

    /**
     * Gemini API 원본 응답에서 텍스트 추출
     * 응답 구조: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
     *
     * @param rawResponse Gemini API 원본 응답 JSON 문자열
     * @return 추출된 텍스트 (JSON 형식)
     */
    private String extractTextFromGeminiResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Gemini API 응답이 비어 있습니다.");
        }

        try {
            JsonObject root = JsonParser.parseString(rawResponse).getAsJsonObject();
            String text = root
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // JSON 펜스(```json ... ```) 가 포함된 경우 제거
            return text.replaceAll("(?s)```json\\s*", "")
                       .replaceAll("```", "")
                       .trim();

        } catch (Exception e) {
            log.error("[AI 요약] 응답 파싱 실패. rawResponse={}", rawResponse, e);
            return "{\"summary\":\"\",\"keywords\":[]}";
        }
    }

    // ── JSON → DTO 변환 ─────────────────────────────────────────────

    /**
     * Gemini 최종 응답 JSON 문자열 → ReviewSummaryResponseDto 변환
     *
     * @param json 최종 요약 JSON 문자열
     * @return 응답 DTO
     */
    private ReviewSummaryResponseDto parseToDto(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            String summary = obj.has("summary") ? obj.get("summary").getAsString() : "";
            List<String> keywords = new ArrayList<>();
            if (obj.has("keywords") && obj.get("keywords").isJsonArray()) {
                obj.getAsJsonArray("keywords").forEach(el -> keywords.add(el.getAsString()));
            }

            return ReviewSummaryResponseDto.builder()
                    .hasReviews(true)
                    .summary(summary)
                    .keywords(keywords)
                    .build();

        } catch (Exception e) {
            log.error("[AI 요약] DTO 변환 실패: {}", e.getMessage(), e);
            return ReviewSummaryResponseDto.builder()
                    .hasReviews(true)
                    .summary("")
                    .keywords(List.of())
                    .build();
        }
    }

    // ── 유틸리티 ────────────────────────────────────────────────────

    /**
     * JsonObject에서 특정 키의 JsonArray를 List<String>으로 변환
     */
    private List<String> jsonArrayToList(JsonObject obj, String key) {
        List<String> result = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            obj.getAsJsonArray(key)
               .forEach(el -> result.add(el.getAsString()));
        }
        return result;
    }

    /**
     * 리스트를 지정 크기의 청크로 분할 (파티셔닝)
     * ex) 250개 리뷰 → [100, 100, 50]
     *
     * @param list      원본 리스트
     * @param chunkSize 청크 크기
     * @return 분할된 리스트의 리스트
     */
    private <T> List<List<T>> partition(List<T> list, int chunkSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            result.add(new ArrayList<>(
                list.subList(i, Math.min(i + chunkSize, list.size()))
            ));
        }
        return result;
    }
}