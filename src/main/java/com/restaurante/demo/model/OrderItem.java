package com.restaurante.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Report Section 3.2: Optimistic Locking
    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference // Previene loop infinito
    private Order order;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "product_id")
    private ProductComponent product;

    private int quantity;
    
    @Column(name = "item_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    // --- Campos para algoritmo CPM ---
    
    private double preparationTime; // en minutos
    @Transient
    private double earlyStart;
    @Transient
    private double earlyFinish;
    @Transient
    private double lateStart;
    @Transient
    private double lateFinish;
    @Transient
    private double slack;

    public Long getOrderId() {
        return (order != null) ? order.getOrderId() : null;
    }
}