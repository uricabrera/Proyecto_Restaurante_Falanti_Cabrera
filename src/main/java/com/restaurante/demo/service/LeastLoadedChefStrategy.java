package com.restaurante.demo.service;

import com.restaurante.demo.dto.ItemStatusUpdateDTO;
import com.restaurante.demo.model.*;
import com.restaurante.demo.repository.ChefRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j // Report Section 6.3
public class LeastLoadedChefStrategy implements RoutingStrategy {

    private final ChefRepository chefRepository;
    private final TimeEstimationService timeEstimationService;
    private static final double URGENCY_WEIGHT = 5.0;

    @Autowired
    public LeastLoadedChefStrategy(ChefRepository chefRepository, TimeEstimationService timeEstimationService) {
        this.chefRepository = chefRepository;
        this.timeEstimationService = timeEstimationService;
    }

    @Override
    public void route(Order order, OrderItem item, ConcurrentHashMap<Long, ChefWorkQueue> chefQueues, SimpMessagingTemplate messagingTemplate) {
        // 1. Calculate routing time via Time Estimation Service
        try {
            timeEstimationService.calculateEstimatedCompletionTime(order);
        } catch (Exception e) {
            log.warn("CPM Calculation failed for order {}. Proceeding with fallback.", order.getOrderId());
        }
        
        ProductComponent component = item.getProduct(); 

        if (!(component instanceof Product)) {
            log.error("Error: Attempted to route composite product {}", component.getName());
            return;
        }

        Product product = (Product) component;
        ChefStation requiredStation = product.getRequiredStation();
        if (requiredStation == null) {
            log.warn("Warning: Product {} has no required station.", product.getName());
            return;
        }

        List<Chef> availableChefs = chefRepository.findByStation(requiredStation);

        if (availableChefs.isEmpty()) {
            log.error("CRITICAL: No chefs found for station: {}", requiredStation);
            return;
        }

        Chef bestChef = null;
        double bestScore = Double.MAX_VALUE;

        // 2. Iterate chefs to find least loaded
        for (Chef chef : availableChefs) {
            ChefWorkQueue queue = chefQueues.computeIfAbsent(chef.getUserId(), k -> new ChefWorkQueue());
            
            double effectiveLoad = queue.getTotalEstimatedTimeInMinutes() * chef.getEfficiency();
            double itemSlack = item.getSlack();
            
            double adjustedSlack = Math.max(0, itemSlack);
            double urgencyFactor = 1.0 / (1.0 + adjustedSlack); 
            double urgencyBonus = urgencyFactor * URGENCY_WEIGHT; 
            double placementScore = effectiveLoad - urgencyBonus;

            log.debug("Chef {} ({}): Load={:.2f}, Eff={:.2f} | Score={:.2f}",
                    chef.getUserId(), requiredStation, queue.getTotalEstimatedTimeInMinutes(), chef.getEfficiency(), placementScore);

            if (placementScore < bestScore) {
                bestScore = placementScore;
                bestChef = chef;
            }
        }

        // 3. Assign best chef and NOTIFY
        if (bestChef != null) {
            ChefWorkQueue targetQueue = chefQueues.get(bestChef.getUserId());
            targetQueue.addItem(item);
            
            // Set status to PREPARING as it enters the queue
            item.setStatus(OrderStatus.PREPARING);
            
            log.info(">>> Item '{}' assigned to Chef {}. New Queue Load: {:.2f} mins.",
                item.getProduct().getName(), bestChef.getUserId(), targetQueue.getTotalEstimatedTimeInMinutes()
            );

            // WebSocket Notification: Notify Kitchen/Chef immediately
            // Report Section 4.1: Wrap in TransactionSynchronization to ensure DB is committed before notifying frontend
            ItemStatusUpdateDTO updateDTO = new ItemStatusUpdateDTO(
                item.getId(),
                order.getOrderId(),
                bestChef.getUserId(),
                bestChef.getNombre(),
                bestChef.getStation(),
                OrderStatus.PREPARING,
                product.getName(),
                product.getPreparationTime() * item.getQuantity()
            );

            sendAfterCommit(messagingTemplate, "/topic/kitchen/orders", updateDTO);

        } else {
            log.error("Error: Could not find a best chef for station {}", requiredStation);
        }
    }

    /**
     * Helper method to send WebSocket messages only after the transaction commits.
     * Report Section 4.1
     */
    private void sendAfterCommit(SimpMessagingTemplate template, String destination, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    template.convertAndSend(destination, payload);
                    log.debug("WS Message sent to {} (After Commit)", destination);
                }
            });
        } else {
            // If no transaction is active (e.g., unit test), send immediately
            template.convertAndSend(destination, payload);
        }
    }
}