package com.restaurante.demo.service;

import com.restaurante.demo.BaseAlgorithmTest;
import com.restaurante.demo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeEstimationServiceTest extends BaseAlgorithmTest {

    @Autowired
    private TimeEstimationService timeEstimationService;

    private Product dough, sauce, toppings, pizza;

    @BeforeEach
    void setUp() {
        setupTestData(); // Configurar productos básicos desde la clase base

        // --- Crear una cadena de dependencias para la pizza ---
        // 1. Preparar masa (3 mins)
        dough = (Product) createSimpleProduct("Pizza Dough", 1.0, 3.0, ChefStation.SOUS_CHEF_STATION, null);
        // 2. Añadir salsa (1 min, depende de la masa)
        sauce = (Product) createSimpleProduct("Tomato Sauce", 0.5, 1.0, ChefStation.SAUCE_STATION, dough.getId());
        // 3. Añadir ingredientes (2 mins, depende de la salsa)
        toppings = (Product) createSimpleProduct("Toppings", 1.5, 2.0, ChefStation.SOUS_CHEF_STATION, sauce.getId());
        // 4. Hornear pizza (8 mins, depende de los ingredientes)
        pizza = (Product) createSimpleProduct("Baked Pizza", 0, 8.0, ChefStation.MAIN_CHEF_STATION, toppings.getId());
    }

    private Order createOrderWithDependencies() {
        Order order = new Order();
        OrderItem doughItem = new OrderItem();
        doughItem.setProduct(dough);
        doughItem.setPreparationTime(dough.getPreparationTime());
        doughItem.setOrder(order);

        OrderItem sauceItem = new OrderItem();
        sauceItem.setProduct(sauce);
        sauceItem.setPreparationTime(sauce.getPreparationTime());
        sauceItem.setOrder(order);

        OrderItem toppingsItem = new OrderItem();
        toppingsItem.setProduct(toppings);
        toppingsItem.setPreparationTime(toppings.getPreparationTime());
        toppingsItem.setOrder(order);

        OrderItem pizzaItem = new OrderItem();
        pizzaItem.setProduct(pizza);
        pizzaItem.setPreparationTime(pizza.getPreparationTime());
        pizzaItem.setOrder(order);

        order.getItems().addAll(List.of(doughItem, sauceItem, toppingsItem, pizzaItem));
        return order;
    }

    @Test
    void testCPM_withSingleComplexOrder() {
        System.out.println("--- Probando CPM con una orden compleja ---");
        Order order = createOrderWithDependencies();

        // Tiempo de ruta crítica esperado = 3 (masa) + 1 (salsa) + 2 (ingredientes) + 8 (horneado) = 14.0 minutos
        double estimatedTime = timeEstimationService.calculateEstimatedCompletionTime(order);

        assertEquals(14.0, estimatedTime, 0.01, "La ruta crítica para la pizza debería ser de 14 minutos.");
        System.out.println("Prueba superada: El tiempo estimado es correcto.");
    }

    @Test
    void testCPM_withIndependentAndDependentItems() {
        System.out.println("--- Probando CPM con una mezcla de ítems ---");
        Order order = createOrderWithDependencies(); // Ruta de la pizza = 14 mins

        // Añadir papas fritas (3 mins, independiente)
        OrderItem friesItem = new OrderItem();
        friesItem.setProduct(fries);
        friesItem.setPreparationTime(fries.getPreparationTime());
        friesItem.setOrder(order);
        order.getItems().add(friesItem);

        // La ruta más larga (ruta crítica) sigue siendo la preparación de la pizza.
        // El tiempo total debe ser determinado por la cadena de tareas más larga, no por la suma de todas las tareas.
        double estimatedTime = timeEstimationService.calculateEstimatedCompletionTime(order);

        assertEquals(14.0, estimatedTime, 0.01, "La ruta crítica debería seguir siendo 14 minutos, ya que la pizza es la tarea más larga.");

        // Verificar el tiempo de holgura para el ítem independiente (papas fritas)
        OrderItem foundFries = order.getItems().stream().filter(i -> i.getProduct().getName().equals("Fries")).findFirst().get();
        assertTrue(foundFries.getSlack() > 0, "Las papas fritas deben tener holgura ya que no están en la ruta crítica.");
        System.out.println("Prueba superada: La ruta crítica es correcta y los ítems independientes tienen holgura.");
    }
}