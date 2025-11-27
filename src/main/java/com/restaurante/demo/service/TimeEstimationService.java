package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.Product;
import com.restaurante.demo.model.ProductComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j // Report Section 6.3
public class TimeEstimationService {

    /**
     * Calcula el tiempo estimado de completado de una orden a traves de un algoritmo de grafo de dependencias.
     * @param order The order to analyze.
     * @return The total estimated time in minutes.
     */
    public double calculateEstimatedCompletionTime(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return 0.0;
        }

        List<OrderItem> items = order.getItems(); 
        Map<Long, OrderItem> itemMap = new HashMap<>(); 
        for (OrderItem item : items) {
            if (item.getProduct() != null && item.getProduct().getId() != null) {
                itemMap.put(item.getProduct().getId(), item); 
            }
        }

        // 1. Construir el grafo de dependencias
        Map<OrderItem, List<OrderItem>> successors = new HashMap<>();
        Map<OrderItem, Integer> predecessorCount = new HashMap<>();
        for (OrderItem item : items) {
            successors.put(item, new ArrayList<>());
            predecessorCount.put(item, 0);
        }

        for (OrderItem item : items) {
            ProductComponent component = item.getProduct();
            if (component instanceof Product) {
                Product product = (Product) component;
                if (product.getPrerequisiteProductId() != null) {
                    OrderItem prerequisiteItem = itemMap.get(product.getPrerequisiteProductId());
                    if (prerequisiteItem != null) {
                        successors.get(prerequisiteItem).add(item);
                        predecessorCount.put(item, predecessorCount.get(item) + 1);
                    }
                }
            }
        }

        // 2. Sorting topologico 
        Queue<OrderItem> queue = new LinkedList<>();
        for (OrderItem item : items) {
            if (predecessorCount.get(item) == 0) {
                queue.add(item);
            }
        }

        List<OrderItem> topologicalOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            OrderItem current = queue.poll();
            topologicalOrder.add(current);
            for (OrderItem successor : successors.get(current)) {
                predecessorCount.put(successor, predecessorCount.get(successor) - 1);
                if (predecessorCount.get(successor) == 0) {
                    queue.add(successor);
                }
            }
        }

        if (topologicalOrder.size() != items.size()) {
            log.error("Ciclo detectado o dependencias faltantes en CPM. Usando estimacion simple (SUM).");
            return items.stream().mapToDouble(i -> i.getPreparationTime() * i.getQuantity()).sum();
        }

        // 3. Forward Pass
        double totalTime = 0;
        for (OrderItem item : topologicalOrder) {
            double maxPredFinishTime = 0;
            ProductComponent component = item.getProduct();
            if (component instanceof Product) {
                Product product = (Product) component;
                if (product.getPrerequisiteProductId() != null) {
                    OrderItem prerequisiteItem = itemMap.get(product.getPrerequisiteProductId());
                    if (prerequisiteItem != null) {
                        maxPredFinishTime = prerequisiteItem.getEarlyFinish();
                    }
                }
            }
            item.setEarlyStart(maxPredFinishTime);
            double totalItemTime = item.getPreparationTime() * item.getQuantity();
            item.setEarlyFinish(item.getEarlyStart() + totalItemTime);
            totalTime = Math.max(totalTime, item.getEarlyFinish());
        }

        // 4. Backward Pass
        for (int i = topologicalOrder.size() - 1; i >= 0; i--) {
            OrderItem item = topologicalOrder.get(i);
            double minSuccStartTime = totalTime;
            if (!successors.get(item).isEmpty()) {
                minSuccStartTime = successors.get(item).stream()
                                                    .mapToDouble(OrderItem::getLateStart)
                                                    .min().orElse(totalTime);
            }
            item.setLateFinish(minSuccStartTime);
            double totalItemTime = item.getPreparationTime() * item.getQuantity();
            item.setLateStart(item.getLateFinish() - totalItemTime);
        }

        // 5. Logging Analysis (Replaced System.out)
        log.info("--- CPM Critical Path Analysis (Order {}) ---", order.getOrderId());
        log.info("Total Estimated Time: {} minutes.", totalTime);
        for (OrderItem item : topologicalOrder) {
            item.setSlack(item.getLateStart() - item.getEarlyStart());
            log.debug("Item: {} | Dur: {} | Slack: {} {}", 
                item.getProduct().getName(), 
                (item.getPreparationTime() * item.getQuantity()), 
                item.getSlack(), 
                (item.getSlack() < 0.001) ? "<- CRITICAL" : ""
            );
        }

        return totalTime;
    }
}