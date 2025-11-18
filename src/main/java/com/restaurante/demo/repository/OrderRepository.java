package com.restaurante.demo.repository;

import com.restaurante.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * THE FINAL FIX: Added the 'DISTINCT' keyword.
     * When using JOIN FETCH on a collection (a One-to-Many relationship like 'items'),
     * Hibernate may return duplicate parent entities (Orders) for each child (OrderItem).
     * The 'DISTINCT' keyword ensures that the final result is a list of unique,
     * fully initialized Order objects, with all their nested associations correctly loaded.
     * This prevents the NullPointerException during JSON serialization.
     *
     * @return A list of unique and fully initialized Order objects.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.status <> 'COMPLETED' AND o.status <> 'REVOKED'")
    List<Order> findAllActiveOrdersForKitchen();
}