package com.deushu.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

/*
 * 決済検証リクエストDTO
 * フロントエンドでPortOne決済が完了した際、発行された決済番号(imp_uid)を受け取ります。
 */
@Getter
@ToString
public class PaymentVerifyRequestDto {
    @NotBlank(message = "決済番号(imp_uid)は必須です。")
    private String impUid; // PortOneサーバー側で生成された実際の決済固有ID
}