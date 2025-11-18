package com.restaurante.demo.service;

import com.restaurante.demo.BaseAlgorithmTest;
import com.restaurante.demo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LeastLoadedChefStrategyTest extends BaseAlgorithmTest {

    @Autowired
    private OrderDispatcher orderDispatcher; // Usando el despachador para acceder a las colas de trabajo
    @Autowired
    private LeastLoadedChefStrategy routingStrategy;

    private Chef grillChefA, grillChefB;

    @BeforeEach
    void setUp() {
        setupTestData(); // Configura chefs de Parrilla, Sous Chef y Pastelería.

        // Añadir un segundo Chef de Parrilla menos eficiente para probar el balanceo de carga
        grillChefA = chefRepository.findByStation(ChefStation.GRILL_STATION).get(0); // Gordon (eficiencia: 1.2)
        grillChefB = createChef("Marco", "grill_chef_b", ChefStation.GRILL_STATION, 0.8);

        // Reinicializar las colas del despachador para incluir al nuevo chef
        orderDispatcher.initializeQueuesForTesting();
    }

    private Order createOrderWithItems(List<ProductComponent> products) {
        Order order = new Order();
        products.forEach(p -> {
            OrderItem item = new OrderItem();
            item.setProduct(p);
            item.setOrder(order);
            item.setPreparationTime(p.getPreparationTime());
            order.getItems().add(item);
        });
        return order;
    }

    @Test
    void testLoadBalancing_withLowLoad() {
        System.out.println("--- Probando Balanceo de Carga con 10 ítems de parrilla ---");
        Order order = createOrderWithItems(
            IntStream.range(0, 10).mapToObj(i -> burgerPatty).toList()
        );

        // Despachar las 10 hamburguesas
        order.getItems().forEach(item -> routingStrategy.route(order, item, orderDispatcher.getChefQueues()));

        ChefWorkQueue queueA = orderDispatcher.getQueueForChef(grillChefA.getUserId());
        ChefWorkQueue queueB = orderDispatcher.getQueueForChef(grillChefB.getUserId());

        // Como el Chef A es más eficiente, debería recibir más trabajo.
        // El algoritmo balancea basado en la carga efectiva (tiempo / eficiencia).
        System.out.printf("Chef A (eficiencia: %.1f) tamaño de cola: %d%n", grillChefA.getEfficiency(), queueA.getItemQueue().size());
        System.out.printf("Chef B (eficiencia: %.1f) tamaño de cola: %d%n", grillChefB.getEfficiency(), queueB.getItemQueue().size());

        assertTrue(queueA.getItemQueue().size() > queueB.getItemQueue().size(), "El chef más eficiente debería tener más ítems en su cola de trabajo.");
        System.out.println("Prueba superada: El trabajo se distribuye según la eficiencia.");
    }

    @Test
    void testLoadBalancing_withHighLoad() {
        System.out.println("--- Probando Balanceo de Carga con 200 ítems de parrilla ---");
        Order order = createOrderWithItems(
            IntStream.range(0, 200).mapToObj(i -> burgerPatty).toList()
        );

        // Despachar las 200 hamburguesas
        order.getItems().forEach(item -> routingStrategy.route(order, item, orderDispatcher.getChefQueues()));

        ChefWorkQueue queueA = orderDispatcher.getQueueForChef(grillChefA.getUserId());
        ChefWorkQueue queueB = orderDispatcher.getQueueForChef(grillChefB.getUserId());

        // La carga efectiva (tiempo total en cola / eficiencia) debería ser muy similar para ambos chefs.
        double effectiveLoadA = queueA.getTotalEstimatedTimeInMinutes() / grillChefA.getEfficiency();
        double effectiveLoadB = queueB.getTotalEstimatedTimeInMinutes() / grillChefB.getEfficiency();

        System.out.printf("Chef A -> Ítems: %d, Tiempo: %.2f mins, Carga Efectiva: %.2f%n",
            queueA.getItemQueue().size(), queueA.getTotalEstimatedTimeInMinutes(), effectiveLoadA);
        System.out.printf("Chef B -> Ítems: %d, Tiempo: %.2f mins, Carga Efectiva: %.2f%n",
            queueB.getItemQueue().size(), queueB.getTotalEstimatedTimeInMinutes(), effectiveLoadB);

        // Verificar si las cargas efectivas están dentro del tiempo de preparación de 1 ítem de diferencia.
        double tolerance = burgerPatty.getPreparationTime() / grillChefB.getEfficiency(); // Usamos al chef menos eficiente como referencia.
        assertTrue(Math.abs(effectiveLoadA - effectiveLoadB) < tolerance, "La carga efectiva en ambos chefs debería estar balanceada.");
        System.out.println("Prueba superada: La carga efectiva está bien balanceada bajo una carga alta.");
    }
}