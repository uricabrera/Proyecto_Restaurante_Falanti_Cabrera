package com.restaurante.demo.service;

import com.restaurante.demo.dto.ItemStatusUpdateDTO;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.OrderStatus;
import com.restaurante.demo.repository.OrderItemRepository;
import com.restaurante.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final List<Observer> observers = new ArrayList<>();
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderDispatcher orderDispatcher; 

    @Autowired
    public OrderService(OrderRepository orderRepository, 
                        OrderItemRepository orderItemRepository, 
                        SimpMessagingTemplate messagingTemplate,
                        OrderDispatcher orderDispatcher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.messagingTemplate = messagingTemplate;
        this.orderDispatcher = orderDispatcher;
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

    @Transactional
    public Order placeOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        
        // Dispatch items immediately
        savedOrder.getItems().forEach(item -> {
            orderDispatcher.dispatch(savedOrder, item);
        });
        
        // Start preparing state logic
        startPreparingOrder(savedOrder.getOrderId());

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

        // FIX: WebSocket Notification for completion AFTER COMMIT
        ItemStatusUpdateDTO update = new ItemStatusUpdateDTO(
            item.getId(),
            item.getOrder().getOrderId(),
            null, 
            "Kitchen", 
            null, 
            OrderStatus.COMPLETED,
            item.getProduct().getName(),
            0.0
        );
        
        sendAfterCommit(messagingTemplate, "/topic/kitchen/orders", update);

        Order parentOrder = orderRepository.findById(item.getOrder().getOrderId())
                .orElseThrow(() -> new RuntimeException("Parent order not found during completion check."));

        // Check if all items are complete
        boolean allOtherItemsComplete = parentOrder.getItems().stream()
            .filter(orderItem -> !orderItem.getId().equals(orderItemId)) 
            .allMatch(orderItem -> orderItem.getStatus() == OrderStatus.COMPLETED);

        if (allOtherItemsComplete && parentOrder.getStatus() == OrderStatus.PREPARING) {
            parentOrder.handleRequest(); // This triggers PreparingState -> CompletedState
            orderRepository.save(parentOrder);
            
            // Notify order completion
            sendAfterCommit(messagingTemplate, "/topic/kitchen/order-complete", parentOrder.getOrderId());
        }

        return item;
    }
    
    public List<Order> getActiveOrders() {
        return orderRepository.findAllActiveOrdersForKitchen();
    }
    
    /**
     * Helper method to send WebSocket messages only after the transaction commits.
     */
    private void sendAfterCommit(SimpMessagingTemplate template, String destination, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    template.convertAndSend(destination, payload);
                }
            });
        } else {
            template.convertAndSend(destination, payload);
        }
    }
}