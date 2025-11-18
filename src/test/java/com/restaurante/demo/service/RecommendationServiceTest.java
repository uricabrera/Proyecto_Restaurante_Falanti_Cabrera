package com.restaurante.demo.service;

import com.restaurante.demo.BaseAlgorithmTest;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.ProductComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecommendationServiceTest extends BaseAlgorithmTest {

    @Autowired
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void createHistoricalOrders(int burgerAndFriesCount, int mealDealCount) {
        // Simular órdenes pasadas para entrenar el algoritmo Apriori
        for (int i = 0; i < burgerAndFriesCount; i++) {
            Order order = new Order();
            order.setItems(List.of(
                createOrderItem(cheeseburger, order),
                createOrderItem(fries, order)
            ));
            orderRepository.save(order);
        }
        for (int i = 0; i < mealDealCount; i++) {
            Order order = new Order();
            order.setItems(List.of(
                createOrderItem(mealDeal, order)
            ));
            orderRepository.save(order);
        }
    }

    private OrderItem createOrderItem(ProductComponent product, Order order) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setOrder(order);
        item.setQuantity(1);
        return item;
    }

    @Test
    void testApriori_withLowLoadAndStrongAssociation() {
        System.out.println("--- Probando Apriori con 10 órdenes (asociación fuerte) ---");
        // 9 de 10 órdenes tienen Hamburguesa con queso + Papas fritas. Este es un vínculo muy fuerte.
        createHistoricalOrders(9, 1);

        // El carrito actual solo tiene una hamburguesa con queso
        Set<Long> currentCartIds = Set.of(cheeseburger.getId());

        // Umbrales bajos debido al pequeño conjunto de datos
        List<ProductComponent> recommendations = recommendationService.generateRecommendations(currentCartIds, 0.1, 0.5);

        assertEquals(1, recommendations.size(), "Debería recomendar exactamente un ítem.");
        assertEquals("Fries", recommendations.get(0).getName(), "Las papas fritas deberían ser recomendadas con una hamburguesa con queso.");
        System.out.println("Prueba superada: Se recomendaron correctamente las papas fritas.");
    }

    @Test
    void testApriori_withHighLoadAndNoStrongAssociation() {
        System.out.println("--- Probando Apriori con 200 órdenes (sin asociación fuerte) ---");
        // Crear 100 órdenes solo de hamburguesas con queso y 100 solo de papas fritas
        for (int i = 0; i < 100; i++) {
            orderRepository.save(new Order(){{ setItems(List.of(createOrderItem(cheeseburger, this))); }});
        }
        for (int i = 0; i < 100; i++) {
            orderRepository.save(new Order(){{ setItems(List.of(createOrderItem(fries, this))); }});
        }

        // El carrito actual tiene una hamburguesa con queso
        Set<Long> currentCartIds = Set.of(cheeseburger.getId());

        // Con una confianza mínima del 50%, no se debería generar ninguna regla (la hamburguesa aparece con papas fritas el 0% de las veces)
        List<ProductComponent> recommendations = recommendationService.generateRecommendations(currentCartIds, 0.1, 0.5);

        assertTrue(recommendations.isEmpty(), "No se debería recomendar ningún ítem ya que no existe asociación.");
        System.out.println("Prueba superada: No se generaron recomendaciones cuando la asociación es débil.");
    }
}