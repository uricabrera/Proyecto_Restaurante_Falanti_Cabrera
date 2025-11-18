package com.restaurante.demo.service;

import com.restaurante.demo.model.*;
import com.restaurante.demo.repository.ChefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public void route(Order order, OrderItem item, ConcurrentHashMap<Long, ChefWorkQueue> chefQueues) {
        // 1. Calcula el tiempo de ruteo a traves del algoritmo de tiempo de estimacion de servicio
        timeEstimationService.calculateEstimatedCompletionTime(order);
        
        ProductComponent component = item.getProduct(); // Consigue los productos presentes en cada orden (ProductComponents)

        
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

        List<Chef> availableChefs = chefRepository.findByStation(requiredStation);

        if (availableChefs.isEmpty()) {
            System.err.println("Warning: no se encontro chefs para: " + requiredStation);
            return;
        }

        Chef bestChef = null;
        double bestScore = Double.MAX_VALUE;

        // 2. Iterar a los chefs para las distintas estaciones
        for (Chef chef : availableChefs) {
            ChefWorkQueue queue = chefQueues.get(chef.getUserId());
            if (queue != null) {
                double effectiveLoad = queue.getTotalEstimatedTimeInMinutes() * chef.getEfficiency();
                double itemSlack = item.getSlack();
                double urgencyFactor = 1.0 / (1.0 + itemSlack); 
                double urgencyBonus = urgencyFactor * URGENCY_WEIGHT; // Verificamos un bono de urgencia para el pedido.
                double placementScore = effectiveLoad - urgencyBonus;

                System.out.printf("Chef %d (%s): Carga=%.2f, Eficiencia=%.2f, Cargaeff=%.2f | Item Slack=%.2f, UrgencyBonus=%.2f -> Puntaje=%.2f%n",
                        chef.getUserId(), requiredStation, queue.getTotalEstimatedTimeInMinutes(), chef.getEfficiency(), effectiveLoad, itemSlack, urgencyBonus, placementScore);

                if (placementScore < bestScore) {
                    bestScore = placementScore;
                    bestChef = chef;
                }
            }
        }

        // 3. Asignar el chef con el mejor score
        if (bestChef != null) {
            ChefWorkQueue targetQueue = chefQueues.get(bestChef.getUserId());
            // Se aniade el item a la Queue con el mejor chef puntuado
            targetQueue.addItem(item);
            System.out.printf(">>> Item '%s' fue enviado a Chef %d (Mejor puntaje: %.2f). Nueva carga del Queue: %.2f mins.%n",
                item.getProduct().getName(), bestChef.getUserId(), bestScore, targetQueue.getTotalEstimatedTimeInMinutes()
            );
        } else {
            System.err.println("Error: No se pudo encontrar un chef para la estacion " + requiredStation);
        }
    }
}