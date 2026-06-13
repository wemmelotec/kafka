package com.poc.orderservice.application.port.in;

import java.math.BigDecimal;

/**
 * Comando de entrada do caso de uso de criação de pedido. Desacopla o
 * caso de uso do DTO HTTP — a camada de aplicação não conhece Jackson nem
 * Bean Validation.
 */
public record CreateOrderCommand(String cpf, BigDecimal salario) {
}
