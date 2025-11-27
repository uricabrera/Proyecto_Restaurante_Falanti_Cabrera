package com.restaurante.demo.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletedState implements OrderState {
    @Override
    public void handle(Order order) {
        log.warn("Order {} is already COMPLETED. Ignoring request.", order.getOrderId());
    }
}