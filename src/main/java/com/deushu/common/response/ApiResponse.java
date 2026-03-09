// =========================================================================
// [ ドゥーシュー ] 共通レスポンスオブジェクト (Global API Wrapper)
// フロントエンドとの通信規格を統一し、一貫したJSONフォーマットを提供するコアクラスです。
// =========================================================================

package com.deushu.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 全てのAPIエンドポイントで返却される共通レスポンス形式を定義します。
 * * @param <T> 実際のペイロード（データ）の型
 */
@Getter
@AllArgsConstructor
// nullの値を持つフィールドはJSONの直列化から除外し、ネットワークの帯域幅を節約します。
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final T data;

    /*
     * 正常処理時のレスポンスを生成します。
     *
     * @param data フロントエンドに渡す実際のデータ
     * @param <T>  データの型
     * @return 成功ステータスとデータを含むApiResponseオブジェクト
     */
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, "COMMON200", "要請が正常に処理されました。", data);
    }

    /*
     * 例外またはエラー発生時のレスポンスを生成します。
     * ユさんの担当である @ControllerAdvice (GlobalExceptionHandler) 等で活用されます。
     *
     * @param code    カスタムエラーコード (例: "ORDER4001")
     * @param message エラーの詳細メッセージ
     * @param data    エラーに関する追加データ (通常はnull)
     * @param <T>     データの型
     * @return 失敗ステータスとエラー情報を含むApiResponseオブジェクト
     */
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}