package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.Product;
import com.restaurante.demo.model.ProductComponent;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TimeEstimationService {

    /**
     * Calcula el tiempo estimado de completado de una orden a traves de un algoritmo de grafo de dependencias.
     *
     * @param order The order to analyze.
     * @return The total estimated time in minutes.
     */
    public double calculateEstimatedCompletionTime(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return 0.0;
        }

        List<OrderItem> items = order.getItems(); // Conseguimos order items
        Map<Long, OrderItem> itemMap = new HashMap<>(); // HashMap con llave id y OrderItem como valor
        for (OrderItem item : items) {
            // Solo contiene productos simples asi que es seguro
            itemMap.put(item.getProduct().getId(), item); // Colocamos los items de la orden en un mapa con llave id del orden item y el item como valor
        }

        // 1. Construir el grafo de dependencias
        Map<OrderItem, List<OrderItem>> successors = new HashMap<>(); // Creamos un hashmap vacio de sucesores Un mapa que guardará para cada producto, una lista de los productos que dependen de él
        Map<OrderItem, Integer> predecessorCount = new HashMap<>(); // Creamos un hashmap vacio de predecesores Un mapa que cuenta cuántos productos necesita cada producto antes de poder ser preparado
        for (OrderItem item : items) {
            successors.put(item, new ArrayList<>()); // Cada item puede tener una lista de sucesores
            predecessorCount.put(item, 0); // Para cada item es necesario saber cuantos predecesores tiene
        }

        for (OrderItem item : items) {
            ProductComponent component = item.getProduct();
            // Solo prerequisitos en productos hoja, no compuestos
            if (component instanceof Product) {
                Product product = (Product) component;
                if (product.getPrerequisiteProductId() != null) { // Si el prerequisito id no es nulo, entonces significa que nuestro item tiene predecesores
                    OrderItem prerequisiteItem = itemMap.get(product.getPrerequisiteProductId()); // conseguimos los valores sucesores
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
            if (predecessorCount.get(item) == 0) { //Si no tiene ningun predecesor, aniadir a la queue
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
                    queue.add(successor); //Procedemos a aniadir el OrderItem sucesor con su respectivo valor de predecesor
                }
            }
        }

        if (topologicalOrder.size() != items.size()) {
            System.err.println("Ciclo detectado.No se puede calcular el tiempo estimado de orden");
            return -1; // Indicar error
        }

        // 3. Forward Pass, una vez que tenemos los grafos creados, ahora podemos analizar el tiempo de estimacion de completado
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
            // El tiempo de preparacion ya esta cuando se crea la orden
            item.setEarlyFinish(item.getEarlyStart() + item.getPreparationTime());
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
            item.setLateStart(item.getLateFinish() - item.getPreparationTime());
        }

        // 5. Realizamos una demostracion en consola de nuestro algoritmo
        System.out.println("--- Analisis de camino critico ---");
        System.out.println("Tiempo total estimado de la orden: " + totalTime + " minutos.");
        for (OrderItem item : topologicalOrder) {
            item.setSlack(item.getLateStart() - item.getEarlyStart());
            System.out.printf(
                "Item: %-20s | PrepTime: %4.1f | ES: %4.1f | EF: %4.1f | LS: %4.1f | LF: %4.1f | Slack: %4.1f %s%n",
                item.getProduct().getName(), item.getPreparationTime(), item.getEarlyStart(),
                item.getEarlyFinish(), item.getLateStart(), item.getLateFinish(),
                item.getSlack(), (item.getSlack() < 0.001) ? "<- CRITICAL" : ""
            );
        }
        System.out.println("------------------------------------");

        return totalTime;
    }
}