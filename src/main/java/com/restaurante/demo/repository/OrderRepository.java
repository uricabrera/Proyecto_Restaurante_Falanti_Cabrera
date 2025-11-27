package com.restaurante.demo.repository;

import com.restaurante.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Updated Query: Now includes 'LEFT JOIN FETCH i.assignedChef' 
     * to ensure the UI can display who is working on what.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "LEFT JOIN FETCH i.assignedChef " +
           "WHERE o.status <> 'COMPLETED' AND o.status <> 'REVOKED'")
    List<Order> findAllActiveOrdersForKitchen();
}