package com.restaurante.demo.repository;

import com.restaurante.demo.model.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductComponent, Long> {
    
    // New method to get only products that should be on the menu
	@Query("SELECT p FROM ProductComponent p WHERE p.isVisibleToClient = true")
	List<ProductComponent> findAllVisibleToClient();

    // Query to get only simple products (leaves) for the composite builder
    @Query("SELECT p FROM Product p")
    List<ProductComponent> findAllSimpleProducts();
}