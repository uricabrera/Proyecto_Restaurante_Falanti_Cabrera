package com.restaurante.demo.service;

import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.ChefStation;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.repository.ChefRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderDispatcher {

    // Hashmap concurrente
    private final ConcurrentHashMap<Long, ChefWorkQueue> chefQueues = new ConcurrentHashMap<>();
    private final RoutingStrategy routingStrategy;
    private final ChefRepository chefRepository;

    @Autowired
    public OrderDispatcher(LeastLoadedChefStrategy routingStrategy, ChefRepository chefRepository) {
        this.routingStrategy = routingStrategy;
        this.chefRepository = chefRepository;
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
        // Limpia las colas existentes antes de reinicializar
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
        routingStrategy.route(order, item, chefQueues);
    }

    /**
     * Conseguir el WorkQueue de un chef por ID
     * @param chefId The ID of the chef.
     * @return The chef's work queue.
     */
    public ChefWorkQueue getQueueForChef(Long chefId) {
        return chefQueues.get(chefId);
    }
}
