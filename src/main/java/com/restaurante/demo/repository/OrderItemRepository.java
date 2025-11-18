package com.restaurante.demo.repository;


//Interfaz que incluye JPA para poder comunicarnos con la base de datos de manera eficiente
import com.restaurante.demo.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
