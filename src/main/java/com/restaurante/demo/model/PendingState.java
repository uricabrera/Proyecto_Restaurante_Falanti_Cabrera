package com.restaurante.demo.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PendingState implements OrderState {
    @Override
    public void handle(Order order) {
        log.info("Order {} is PENDING. Moving to PREPARING.", order.getOrderId());
        order.setState(new PreparingState());
    }
}