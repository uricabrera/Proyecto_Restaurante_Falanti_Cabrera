package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderStatus;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KitchenDisplay implements Observer {

    private final OrderService orderService;
    private final OrderDispatcher orderDispatcher;

    @Autowired
    public KitchenDisplay(OrderService orderService, OrderDispatcher orderDispatcher) {
        this.orderService = orderService;
        this.orderDispatcher = orderDispatcher;
    }

    @PostConstruct
    public void subscribe() {
        orderService.register(this);
    }

    @Override
    public void update(Order order) {
        System.out.println("KITCHEN DISPLAY: New/Updated Order Received - ID: " + order.getOrderId());
        
        if (order.getStatus() == OrderStatus.PENDING) {
            
            order.getItems().forEach(item -> orderDispatcher.dispatch(order, item));

            orderService.startPreparingOrder(order.getOrderId());
        }
    }
}