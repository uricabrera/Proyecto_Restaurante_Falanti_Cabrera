package com.restaurante.demo.model;

public class CompletedState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order " + order.getOrderId() + " is already COMPLETED.");
    }
}