package com.restaurante.demo.service;
import com.restaurante.demo.model.Order;

public interface Observer {
    void update(Order order);
}
