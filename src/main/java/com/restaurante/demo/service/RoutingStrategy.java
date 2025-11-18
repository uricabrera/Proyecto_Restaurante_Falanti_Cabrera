package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.ConcurrentHashMap;

public interface RoutingStrategy {
    // Now accepts the messaging template to trigger real-time updates upon routing
    void route(Order order, OrderItem item, ConcurrentHashMap<Long, ChefWorkQueue> chefQueues, SimpMessagingTemplate messagingTemplate);
}