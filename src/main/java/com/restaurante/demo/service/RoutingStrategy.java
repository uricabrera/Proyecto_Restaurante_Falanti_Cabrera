package com.restaurante.demo.service;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import java.util.concurrent.ConcurrentHashMap;

public interface RoutingStrategy {
    // Now accepts the full order for context
    void route(Order order, OrderItem item, ConcurrentHashMap<Long, ChefWorkQueue> chefQueues);
}