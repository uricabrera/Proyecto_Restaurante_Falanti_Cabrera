package com.restaurante.demo.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PreparingState implements OrderState {
    @Override
    public void handle(Order order) {
        log.info("Order {} is PREPARING. Moving to COMPLETED.", order.getOrderId());
        order.setState(new CompletedState());
    }
}