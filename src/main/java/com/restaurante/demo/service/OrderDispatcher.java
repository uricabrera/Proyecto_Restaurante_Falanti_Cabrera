package com.restaurante.demo.service;

import com.restaurante.demo.dto.ChefQueueDTO;
import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.repository.ChefRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrderDispatcher {

    // Thread-safe map for chef queues
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

    /**
     * Al iniciar, crear una WorkQueue para cada chef
     */
    @PostConstruct
    private void initializeQueues() {
        System.out.println("Initializing Chef Queues...");
        List<Chef> allChefs = chefRepository.findAll();
        allChefs.forEach(chef -> {
            chefQueues.put(chef.getUserId(), new ChefWorkQueue());
            System.out.println("Created queue for Chef ID: " + chef.getUserId());
        });
    }
    
    public void initializeQueuesForTesting() {
        chefQueues.clear();
        initializeQueues();
    }
    
    public ConcurrentHashMap<Long, ChefWorkQueue> getChefQueues() {
        return chefQueues;
    }

    /**
     * Despachar item utilizando estrategia de ruteo
     * @param item The order item to be dispatched.
     */
    public void dispatch(Order order, OrderItem item) {
        // Pass the messaging template to the strategy so it can notify the specific chef immediately
        routingStrategy.route(order, item, chefQueues, messagingTemplate);
    }

    /**
     * Conseguir el WorkQueue de un chef por ID
     * @param chefId The ID of the chef.
     * @return The chef's work queue.
     */
    public ChefWorkQueue getQueueForChef(Long chefId) {
        // Compute if absent to prevent NullPointer if a new chef was added after startup
        return chefQueues.computeIfAbsent(chefId, k -> new ChefWorkQueue());
    }

    public ChefQueueDTO getChefQueueDTO(Long chefId) {
        ChefWorkQueue queue = chefQueues.get(chefId);
        if (queue == null) {
            return new ChefQueueDTO(List.of(), 0.0);
        }
        List<OrderItem> currentQueue = queue.getItemQueue().stream().collect(Collectors.toUnmodifiableList());
        return new ChefQueueDTO(currentQueue, queue.getTotalEstimatedTimeInMinutes());
    }
}