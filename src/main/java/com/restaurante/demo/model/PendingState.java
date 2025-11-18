package com.restaurante.demo.model;

public class PendingState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order " + order.getOrderId() + " is PENDING. Moving to PREPARING.");
        order.setState(new PreparingState());
    }
}
