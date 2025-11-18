package com.restaurante.demo.model;

public interface OrderState {
    void handle(Order order);
}
