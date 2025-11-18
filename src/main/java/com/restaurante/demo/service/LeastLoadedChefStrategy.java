package com.restaurante.demo.service;

import com.restaurante.demo.dto.ItemStatusUpdateDTO;
import com.restaurante.demo.model.*;
import com.restaurante.demo.repository.ChefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// Implementa la logica de ruteo a traves de un balanceador de carga con RoutingStrategy

@Component
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
        // 1. Calcula el tiempo de ruteo a traves del algoritmo de tiempo de estimacion de servicio
        timeEstimationService.calculateEstimatedCompletionTime(order);
        
        ProductComponent component = item.getProduct(); 

        if (!(component instanceof Product)) {
            System.err.println("Error: Se intento enrutar un producto compuesto " + component.getName());
            return;
        }

        Product product = (Product) component;
        ChefStation requiredStation = product.getRequiredStation();
        if (requiredStation == null) {
            System.err.println("Warning: Producto " + product.getName() + " no tiene estacion.");
            return;
        }

        // Get chefs for specific station
        List<Chef> availableChefs = chefRepository.findByStation(requiredStation);

        if (availableChefs.isEmpty()) {
            System.err.println("Warning: no se encontro chefs para: " + requiredStation);
            return;
        }

        Chef bestChef = null;
        double bestScore = Double.MAX_VALUE;

        // 2. Iterar a los chefs para las distintas estaciones
        for (Chef chef : availableChefs) {
            // Ensure queue exists
            ChefWorkQueue queue = chefQueues.computeIfAbsent(chef.getUserId(), k -> new ChefWorkQueue());
            
            double effectiveLoad = queue.getTotalEstimatedTimeInMinutes() * chef.getEfficiency();
            double itemSlack = item.getSlack();
            
            double adjustedSlack = Math.max(0, itemSlack);
            double urgencyFactor = 1.0 / (1.0 + adjustedSlack); 
            double urgencyBonus = urgencyFactor * URGENCY_WEIGHT; 
            double placementScore = effectiveLoad - urgencyBonus;

            System.out.printf("Chef %d (%s): Carga=%.2f, Eficiencia=%.2f | Score=%.2f%n",
                    chef.getUserId(), requiredStation, queue.getTotalEstimatedTimeInMinutes(), chef.getEfficiency(), placementScore);

            if (placementScore < bestScore) {
                bestScore = placementScore;
                bestChef = chef;
            }
        }

        // 3. Asignar el chef con el mejor score y NOTIFICAR
        if (bestChef != null) {
            ChefWorkQueue targetQueue = chefQueues.get(bestChef.getUserId());
            targetQueue.addItem(item);
            
            // Set status to PREPARING as it enters the queue
            item.setStatus(OrderStatus.PREPARING);
            
            System.out.printf(">>> Item '%s' assigned to Chef %d. New Queue Load: %.2f mins.%n",
                item.getProduct().getName(), bestChef.getUserId(), targetQueue.getTotalEstimatedTimeInMinutes()
            );

            // WebSocket Notification: Notify Kitchen/Chef immediately
            // FIX: Wrap in TransactionSynchronization to ensure DB is committed before notifying frontend
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
            System.err.println("Error: No se pudo encontrar un chef para la estacion " + requiredStation);
        }
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
                    System.out.println("WS Message sent to " + destination + " (After Commit)");
                }
            });
        } else {
            // If no transaction is active, send immediately
            template.convertAndSend(destination, payload);
        }
    }
}