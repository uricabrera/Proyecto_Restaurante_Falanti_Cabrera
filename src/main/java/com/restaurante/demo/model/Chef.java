package com.restaurante.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;


// Establecemos las estaciones de chef y eficiencia como campos importantes para el mismo

@Entity
@Getter
@Setter
public class Chef extends Usuario {
    @Enumerated(EnumType.STRING)
    private ChefStation station;

    private double efficiency = 1.0;
}