package com.restaurante.demo.controller;

import com.restaurante.demo.dto.ChefQueueDTO;
import com.restaurante.demo.dto.LoginRequestDTO;
import com.restaurante.demo.dto.LoginResponseDTO;
import com.restaurante.demo.model.*;
import com.restaurante.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class RestaurantController {

    private final OrderProcessingFacade orderFacade;
    private final OrderService orderService;
    private final OrderDispatcher orderDispatcher;
    private final RecommendationService recommendationService;
    private final ProductService productService;
    private final UsuarioService usuarioService;

    @Autowired
    public RestaurantController(
            OrderProcessingFacade orderFacade, OrderService orderService,
            OrderDispatcher orderDispatcher, RecommendationService recommendationService,
            ProductService productService, UsuarioService usuarioService) {
        this.orderFacade = orderFacade;
        this.orderService = orderService;
        this.orderDispatcher = orderDispatcher;
        this.recommendationService = recommendationService;
        this.productService = productService;
        this.usuarioService = usuarioService;
    }

    // =================================================================
    // ==               Endpoints de Productos               ==
    // =================================================================

    @PostMapping("/products")
    public ResponseEntity<ProductComponent> createProduct(@RequestBody ProductComponent product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductComponent> updateProduct(@PathVariable Long id, @RequestBody ProductComponent productDetails) {
        return ResponseEntity.ok(productService.updateProduct(id, productDetails));
    }

    @GetMapping("/products/all")
    public ResponseEntity<List<ProductComponent>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    // Se diferencia de /products/all porque nos permite conseguir productos visibles

    @GetMapping("/products")
    public ResponseEntity<List<ProductComponent>> getVisibleProducts() {
        return ResponseEntity.ok(productService.getVisibleProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductComponent> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    // =================================================================
    // ==                 Endpoints de Cocina, Clientes y Chefs                  ==
    // =================================================================

    @PostMapping("/client/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<Long, Integer> itemDetails) {
        try {
            return ResponseEntity.ok(orderFacade.createAndPlaceOrder(itemDetails)); // Ejemplo de utilización de OrderProcessingFacade
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating order: " + e.getMessage());
        }
    }
    

    @GetMapping("/client/orders/{orderId}/status")
    public ResponseEntity<Order> getOrderStatus(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    
    // Recomendaciones es para el final de algoritmos

    @GetMapping("/client/recommendations")
    public ResponseEntity<List<ProductComponent>> getRecommendations(@RequestParam Set<Long> currentItemIds) {
        double minSupport = 0.01;
        double minConfidence = 0.1;
        List<ProductComponent> recommendations = recommendationService.generateRecommendations(currentItemIds, minSupport, minConfidence);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/kitchen/orders")
    public ResponseEntity<List<Order>> getAllActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }
    
    
    // Post que le permite al chef establecer una orden como completada

    @PostMapping("/chef/items/{itemId}/complete")
    public ResponseEntity<?> completeOrderItem(@PathVariable Long itemId) {
        try {
            orderService.completeOrderItem(itemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error completing item: " + e.getMessage());
        }
    }

    // =================================================================
    // ==                 Endpoints de Login              ==
    // =================================================================

    @PostMapping("/chefs")
    public ResponseEntity<Chef> createChef(@RequestBody Chef chef) {
        return ResponseEntity.ok(usuarioService.createChef(chef));
    }

    @GetMapping("/chefs")
    public ResponseEntity<List<Chef>> getAllChefs() {
        return ResponseEntity.ok(usuarioService.getAllChefs());
    }

    @PutMapping("/chefs/{id}")
    public ResponseEntity<Chef> updateChef(@PathVariable Long id, @RequestBody Chef chefDetails) {
        return ResponseEntity.ok(usuarioService.updateChef(id, chefDetails));
    }

    @PostMapping("/clientes")
    public ResponseEntity<Cliente> createCliente(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(usuarioService.createCliente(cliente));
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<Cliente>> getAllClientes() {
        return ResponseEntity.ok(usuarioService.getAllClientes());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            LoginResponseDTO response = usuarioService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }
}