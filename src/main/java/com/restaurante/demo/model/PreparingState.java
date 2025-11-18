package com.restaurante.demo.model;

public class PreparingState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order " + order.getOrderId() + " is PREPARING. Moving to COMPLETED.");
        order.setState(new CompletedState());
    }
}