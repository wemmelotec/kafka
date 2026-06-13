package com.poc.orderservice.adapters.in.web;

import com.poc.orderservice.application.port.in.CreateOrderCommand;
import com.poc.orderservice.application.port.in.CreateOrderUseCase;
import com.poc.orderservice.domain.Order;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada (web). Traduz HTTP em chamadas ao input port
 * {@link CreateOrderUseCase} e não conhece nenhuma regra de negócio.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
    	
        this.createOrderUseCase = createOrderUseCase;
        
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    	
        Order order = createOrderUseCase.createOrder(new CreateOrderCommand(request.cpf(), request.salario()));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateOrderResponse.received(order.id()));
        
    }
}
