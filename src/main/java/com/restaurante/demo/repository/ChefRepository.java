package com.restaurante.demo.repository;

import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.ChefStation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


// Interfaz que incluye JPA para poder comunicarnos con la base de datos de manera eficiente

public interface ChefRepository extends JpaRepository<Chef, Long> {
    /**
     * Finds all chefs assigned to a specific station.
     * This is crucial for the new load balancing strategy.
     * @param station The chef station to search for.
     * @return A list of chefs.
     */
    List<Chef> findByStation(ChefStation station);
}
