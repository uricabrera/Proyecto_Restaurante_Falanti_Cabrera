package com.restaurante.demo.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("SIMPLE") // Estrategia SingleTable
@Getter
@Setter
@NoArgsConstructor
public class Product extends ProductComponent {

    private double preparationTime;

    private Long prerequisiteProductId; 

    @Enumerated(EnumType.STRING)
    private ChefStation requiredStation;

    // Metodo abstracto implementado
    @Override
    public double getPreparationTime() {
        return this.preparationTime;
    }
}