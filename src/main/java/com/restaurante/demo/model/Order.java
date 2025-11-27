package com.restaurante.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    // Report Section 3.2: Optimistic Locking
    // Prevents "Lost Updates" in high-concurrency scenarios
    @Version
    private Integer version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference // Resolver loop infinito de items
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Transient // Implementamos OrderState dentro de nuestras ordenes
    @JsonIgnore
    private OrderState state;

    public Order() {
        this.status = OrderStatus.PENDING;
        this.state = new PendingState();
    }

    public void setState(OrderState state) {
        this.state = state;
        // El Status ENUM es el que persiste en la base de datos
        if (state instanceof PendingState) {
            this.status = OrderStatus.PENDING;
        } else if (state instanceof PreparingState) {
            this.status = OrderStatus.PREPARING;
        } else if (state instanceof CompletedState) {
            this.status = OrderStatus.COMPLETED;
        }
    }
    
    @PostLoad
    public void initState() {
        if (this.status == OrderStatus.PENDING) {
            this.state = new PendingState();
        } else if (this.status == OrderStatus.PREPARING) {
            this.state = new PreparingState();
        } else if (this.status == OrderStatus.COMPLETED) {
            this.state = new CompletedState();
        }
    }

    public void handleRequest() {
    	if (this.state == null) {
            initState();
        }
        this.state.handle(this);
    }
    
    @JsonIgnore
    public double getTotalPrice() {
        return items.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}