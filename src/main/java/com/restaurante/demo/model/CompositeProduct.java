package com.restaurante.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("COMPOSITE") // SINGLE_TABLE estrategia
@Getter
@Setter
public class CompositeProduct extends ProductComponent {
	
	
	// Logica para obtener todos los productos que corresponden a un producto compuesto

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "composite_product_items",
        joinColumns = @JoinColumn(name = "parent_product_id"),
        inverseJoinColumns = @JoinColumn(name = "child_product_id")
    )
    private List<ProductComponent> children = new ArrayList<>();

    // El tiempo de preparacion es la suma de todos los productos hijos del producto compuesto
    @Override
    public double getPreparationTime() {
        // Esto es una simplificación, habría que usar path critico para la implementacion del algoritmo de tiempo estimado. Implementar luego
        return children.stream()
            .mapToDouble(ProductComponent::getPreparationTime)
            .sum();
    }

    public void addComponent(ProductComponent component) {
        children.add(component);
    }

    public void removeComponent(ProductComponent component) {
        children.remove(component);
    }
}