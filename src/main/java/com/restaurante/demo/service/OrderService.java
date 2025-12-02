package com.restaurante.demo.service;

import com.restaurante.demo.dto.ItemStatusUpdateDTO;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.OrderStatus;
import com.restaurante.demo.repository.OrderItemRepository;
import com.restaurante.demo.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderService {
    
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



    @Transactional
    public Order placeOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        log.info("Order placed: {}", savedOrder.getOrderId());
        
        savedOrder.getItems().forEach(item -> {
            orderDispatcher.dispatch(savedOrder, item);
        });
        
        startPreparingOrder(savedOrder.getOrderId());

        
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
            log.debug("Transitioning Order {} to PREPARING", orderId);
            order.handleRequest();
            orderRepository.save(order);
        }
    }

    /**
     * Modified to include security check: Only the assigned chef can complete the item.
     */
    @Transactional
    public OrderItem completeOrderItem(Long orderItemId, Long requestingChefId) {
        try {
            OrderItem item = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new RuntimeException("OrderItem not found with id: " + orderItemId));
            
            // --- SECURITY CHECK ---
            if (item.getAssignedChef() != null && !item.getAssignedChef().getUserId().equals(requestingChefId)) {
                log.warn("Chef {} attempted to complete item {} assigned to Chef {}", 
                        requestingChefId, orderItemId, item.getAssignedChef().getUserId());
                throw new SecurityException("You are not assigned to this item.");
            }
            // ----------------------

            if (item.getStatus() == OrderStatus.COMPLETED) {
                log.warn("Item {} already completed. Skipping.", orderItemId);
                return item;
            }

            item.setStatus(OrderStatus.COMPLETED);
            orderItemRepository.save(item);

            log.info("Item {} ({}) completed by Chef {}", item.getId(), item.getProduct().getName(), requestingChefId);

            ItemStatusUpdateDTO update = new ItemStatusUpdateDTO(
                item.getId(),
                item.getOrder().getOrderId(),
                requestingChefId, 
                item.getAssignedChef() != null ? item.getAssignedChef().getNombre() : "Unknown", 
                null, 
                OrderStatus.COMPLETED,
                item.getProduct().getName(),
                0.0
            );
            
            sendAfterCommit(messagingTemplate, "/topic/kitchen/orders", update);

            Order parentOrder = orderRepository.findById(item.getOrder().getOrderId())
                    .orElseThrow(() -> new RuntimeException("Parent order not found during completion check."));

            boolean allOtherItemsComplete = parentOrder.getItems().stream()
                .filter(orderItem -> !orderItem.getId().equals(orderItemId)) 
                .allMatch(orderItem -> orderItem.getStatus() == OrderStatus.COMPLETED);

            if (allOtherItemsComplete && parentOrder.getStatus() == OrderStatus.PREPARING) {
                log.info("All items for Order {} complete. Finishing order.", parentOrder.getOrderId());
                parentOrder.handleRequest(); 
                orderRepository.save(parentOrder);
                
                sendAfterCommit(messagingTemplate, "/topic/kitchen/order-complete", parentOrder.getOrderId());
            }

            return item;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic Lock Failure on Item {}", orderItemId);
            throw e;
        }
    }
    
    // Fallback for simple calls without ID check (if needed internally)
    @Transactional
    public OrderItem completeOrderItem(Long orderItemId) {
        return completeOrderItem(orderItemId, -1L); // Bypass check? Or throw error. 
        // For safety, let's just delegate to the one above or require ID. 
        // Given the Controller change, this might not be called from API anymore.
    }
    
    public List<Order> getActiveOrders() {
        return orderRepository.findAllActiveOrdersForKitchen();
    }
    
    private void sendAfterCommit(SimpMessagingTemplate template, String destination, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    template.convertAndSend(destination, payload);
                    log.debug("WS Sent to {} (After Commit)", destination);
                }
            });
        } else {
            template.convertAndSend(destination, payload);
        }
    }
}