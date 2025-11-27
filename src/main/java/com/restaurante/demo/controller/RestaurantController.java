package com.restaurante.demo.controller;

import com.restaurante.demo.dto.LoginRequestDTO;
import com.restaurante.demo.dto.LoginResponseDTO;
import com.restaurante.demo.model.*;
import com.restaurante.demo.repository.UsuarioRepository;
import com.restaurante.demo.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Slf4j
public class RestaurantController {

    private final OrderProcessingFacade orderFacade;
    private final OrderService orderService;
    private final OrderDispatcher orderDispatcher;
    private final RecommendationService recommendationService;
    private final ProductService productService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Autowired
    public RestaurantController(
            OrderProcessingFacade orderFacade, OrderService orderService,
            OrderDispatcher orderDispatcher, RecommendationService recommendationService,
            ProductService productService, UsuarioService usuarioService,
            UsuarioRepository usuarioRepository,
            AuthenticationManager authenticationManager) {
        this.orderFacade = orderFacade;
        this.orderService = orderService;
        this.orderDispatcher = orderDispatcher;
        this.recommendationService = recommendationService;
        this.productService = productService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
    }

    // =================================================================
    // ==                Endpoints de Productos (ADMIN)               ==
    // =================================================================

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductComponent> createProduct(@RequestBody ProductComponent product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductComponent> updateProduct(@PathVariable Long id, @RequestBody ProductComponent productDetails) {
        return ResponseEntity.ok(productService.updateProduct(id, productDetails));
    }

    @GetMapping("/products/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductComponent>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    @GetMapping("/products")
    public ResponseEntity<List<ProductComponent>> getVisibleProducts() {
        return ResponseEntity.ok(productService.getVisibleProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductComponent> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    // =================================================================
    // ==             Endpoints de Cocina, Clientes y Chefs           ==
    // =================================================================

    @PostMapping("/client/orders")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public ResponseEntity<?> createOrder(@RequestBody Map<Long, Integer> itemDetails) {
        try {
            return ResponseEntity.ok(orderFacade.createAndPlaceOrder(itemDetails));
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.badRequest().body("Error creating order: " + e.getMessage());
        }
    }
    
    @GetMapping("/client/orders/{orderId}/status")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public ResponseEntity<Order> getOrderStatus(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/client/recommendations")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public ResponseEntity<List<ProductComponent>> getRecommendations(@RequestParam Set<Long> currentItemIds) {
        double minSupport = 0.01;
        double minConfidence = 0.1;
        List<ProductComponent> recommendations = recommendationService.generateRecommendations(currentItemIds, minSupport, minConfidence);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/kitchen/orders")
    @PreAuthorize("hasRole('CHEF') or hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }
    
    @PostMapping("/chef/items/{itemId}/complete")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<?> completeOrderItem(@PathVariable Long itemId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Usuario user = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!(user instanceof Chef)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only chefs can complete items.");
            }
            
            // Pass the chef ID to service to enforce assignment logic
            orderService.completeOrderItem(itemId, user.getUserId());
            
            return ResponseEntity.ok().build();
            
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Item was modified by another user. Please refresh.");
        } catch (SecurityException e) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error completing item", e);
            return ResponseEntity.badRequest().body("Error completing item: " + e.getMessage());
        }
    }

    // =================================================================
    // ==                  Endpoints de Login                         ==
    // =================================================================

    @PostMapping("/chefs")
    public ResponseEntity<Chef> createChef(@RequestBody Chef chef) {
        return ResponseEntity.ok(usuarioService.createChef(chef));
    }

    @GetMapping("/chefs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Chef>> getAllChefs() {
        return ResponseEntity.ok(usuarioService.getAllChefs());
    }

    @PutMapping("/chefs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Chef> updateChef(@PathVariable Long id, @RequestBody Chef chefDetails) {
        return ResponseEntity.ok(usuarioService.updateChef(id, chefDetails));
    }

    @PostMapping("/clientes")
    public ResponseEntity<Cliente> createCliente(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(usuarioService.createCliente(cliente));
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Cliente>> getAllClientes() {
        return ResponseEntity.ok(usuarioService.getAllClientes());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, request, response);

            LoginResponseDTO dto = usuarioService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            log.error("Login failed for user: " + loginRequest.getUsername(), e);
            return ResponseEntity.status(401).build();
        }
    }
}