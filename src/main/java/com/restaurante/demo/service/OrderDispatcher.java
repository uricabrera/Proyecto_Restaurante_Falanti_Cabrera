package com.restaurante.demo.service;

import com.restaurante.demo.dto.ChefQueueDTO;
import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.repository.ChefRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j // Report Section 6.3
public class OrderDispatcher {

    // Report Section 2.2.2: ConcurrentHashMap used for thread-safe access to station queues
    private final ConcurrentHashMap<Long, ChefWorkQueue> chefQueues = new ConcurrentHashMap<>();
    private final RoutingStrategy routingStrategy;
    private final ChefRepository chefRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public OrderDispatcher(RoutingStrategy routingStrategy, ChefRepository chefRepository, SimpMessagingTemplate messagingTemplate) {
        this.routingStrategy = routingStrategy;
        this.chefRepository = chefRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    private void initializeQueues() {
        log.info("Initializing Chef Queues...");
        List<Chef> allChefs = chefRepository.findAll();
        allChefs.forEach(chef -> {
            chefQueues.put(chef.getUserId(), new ChefWorkQueue());
            log.debug("Created queue for Chef ID: {}", chef.getUserId());
        });
    }
    
    public void initializeQueuesForTesting() {
        chefQueues.clear();
        initializeQueues();
    }
    
    public ConcurrentHashMap<Long, ChefWorkQueue> getChefQueues() {
        return chefQueues;
    }

    public void dispatch(Order order, OrderItem item) {
        // Pass the messaging template to the strategy so it can notify the specific chef immediately
        try {
            routingStrategy.route(order, item, chefQueues, messagingTemplate);
        } catch (Exception e) {
            log.error("Failed to dispatch item {} for order {}", item.getId(), order.getOrderId(), e);
        }
    }

    public ChefWorkQueue getQueueForChef(Long chefId) {
        return chefQueues.computeIfAbsent(chefId, k -> new ChefWorkQueue());
    }

    public ChefQueueDTO getChefQueueDTO(Long chefId) {
        ChefWorkQueue queue = chefQueues.get(chefId);
        if (queue == null) {
            return new ChefQueueDTO(List.of(), 0.0);
        }
        // Return unmodifiable view to protect internal queue state
        List<OrderItem> currentQueue = queue.getItemQueue().stream().collect(Collectors.toUnmodifiableList());
        return new ChefQueueDTO(currentQueue, queue.getTotalEstimatedTimeInMinutes());
    }
}