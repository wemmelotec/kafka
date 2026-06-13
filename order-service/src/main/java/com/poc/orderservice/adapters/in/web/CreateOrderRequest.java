package com.poc.orderservice.adapters.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * DTO de entrada do endpoint {@code POST /api/orders}. Pertence ao adapter web —
 * o domínio e o caso de uso nunca recebem este tipo diretamente.
 */
public record CreateOrderRequest(

        @NotBlank(message = "cpf é obrigatório")
        @Pattern(
                regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11})$",
                message = "cpf deve estar no formato 000.000.000-00 ou 00000000000"
        )
        String cpf,

        @NotNull(message = "salario é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "salario deve ser maior que zero")
        BigDecimal salario
) {
}
