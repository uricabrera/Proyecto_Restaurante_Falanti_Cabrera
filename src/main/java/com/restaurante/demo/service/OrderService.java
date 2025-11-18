package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.OrderStatus;
import com.restaurante.demo.repository.OrderItemRepository;
import com.restaurante.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final List<Observer> observers = new ArrayList<>();
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public void register(Observer o) {
        observers.add(o);
    }

    public void unregister(Observer o) {
        observers.remove(o);
    }

    public void notifyObservers(Order order) {
        for (Observer observer : observers) {
            observer.update(order);
        }
    }

    public Order placeOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        notifyObservers(savedOrder);
        return savedOrder;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
    }

    @Transactional
    public void startPreparingOrder(Long orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.handleRequest(); // This triggers PendingState -> PreparingState
            orderRepository.save(order);
        }
    }

    @Transactional
    public OrderItem completeOrderItem(Long orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found with id: " + orderItemId));
        
        // This item is now complete
        item.setStatus(OrderStatus.COMPLETED);
        orderItemRepository.save(item);

        Order parentOrder = orderRepository.findById(item.getOrder().getOrderId())
                .orElseThrow(() -> new RuntimeException("Parent order not found during completion check."));

        
        // Instead of checking all items (including the potentially stale one),
        
        boolean allOtherItemsComplete = parentOrder.getItems().stream()
            .filter(orderItem -> !orderItem.getId().equals(orderItemId)) // Exclude the item we just completed
            .allMatch(orderItem -> orderItem.getStatus() == OrderStatus.COMPLETED);

        // If the item we just finished is the last one, and the order is being prepared,
        // then the entire order is now ready to be completed.
        if (allOtherItemsComplete && parentOrder.getStatus() == OrderStatus.PREPARING) {
            parentOrder.handleRequest(); // This triggers PreparingState -> CompletedState
            orderRepository.save(parentOrder);
        }

        return item;
    }
    
    public List<Order> getActiveOrders() {
        return orderRepository.findAllActiveOrdersForKitchen();
    }
}