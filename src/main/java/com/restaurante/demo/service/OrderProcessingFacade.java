package com.restaurante.demo.service;

import com.restaurante.demo.model.CompositeProduct;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.ProductComponent;
import com.restaurante.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OrderProcessingFacade {

    private final OrderService orderService;
    private final ProductRepository productRepository;

    @Autowired
    public OrderProcessingFacade(OrderService orderService, ProductRepository productRepository) {
        this.orderService = orderService;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createAndPlaceOrder(Map<Long, Integer> itemDetails) {
        OrderBuilder builder = new OrderBuilder(); // Llama al OrderBuilder
        Order newOrder = builder.create().build(); 

        for (Map.Entry<Long, Integer> entry : itemDetails.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            ProductComponent product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            
            // Decomponer items hoja en caso de items compuestos
            addItemsToOrder(newOrder, product, quantity);
        }

        return orderService.placeOrder(newOrder);
    }

    private void addItemsToOrder(Order order, ProductComponent component, int quantity) {
        if (component instanceof CompositeProduct) {
            // Si es compuesto, recursivamente aniadir hijos
            CompositeProduct composite = (CompositeProduct) component;
            for (ProductComponent child : composite.getChildren()) {
                addItemsToOrder(order, child, quantity);
            }
        } else {
            // Si es un producto simple, aniadir OrderItem
            OrderItem item = new OrderItem();
            item.setProduct(component);
            item.setQuantity(quantity);
            item.setOrder(order);
            // Tiempo de preparacion
            item.setPreparationTime(component.getPreparationTime());
            order.getItems().add(item);
        }
    }
}