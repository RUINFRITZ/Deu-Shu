package com.deushu.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/*
 * 決済（注文）リクエストDTO
 * どの店舗で、どの商品を、何個買うのかをフロントエンドから受け取ります。
 */
@Getter
@ToString
public class OrderCreateRequestDto {

    @NotNull(message = "対象店舗のIDは必須です。")
    private Long storeId; // FR-P01: 1回の注文は1つの店舗に限定

    @NotEmpty(message = "カートに商品が存在しません。")
    @Valid // 内部リストのバリデーションも再帰的にチェック
    private List<CartItemDto> cartItems; // 1:N 複数商品リスト

    @NotNull(message = "注文の合計金額が必要です。")
    @Min(value = 1, message = "合計金額は1円以上である必要があります。")
    private Integer totalPrice; // 改ざん防止のためのフロントエンド計算値（後でサーバー側と再検証します）

    /*
     * カート内の個別商品情報を格納する内部クラス
     */
    @Getter
    @ToString
    public static class CartItemDto {
        @NotNull(message = "商品IDは必須です。")
        private Long itemId;

        @NotNull(message = "数量は必須です。")
        @Min(value = 1, message = "数量は1個以上を指定してください。")
        private Integer quantity;
    }
}