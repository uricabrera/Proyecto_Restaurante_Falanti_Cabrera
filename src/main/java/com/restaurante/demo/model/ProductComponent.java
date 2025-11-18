package com.restaurante.demo.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


// El Inheritance strategy que vamos a utilizar es crear una tabla por cada clase que herede ProductComponent
// La forma en la que vamos a saber en que tabla (ProductComponent,Proudct o CompositeProduct) es a traves del product_type
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "product_type")
@Getter
@Setter
// Decoradores para serializar/deserializar JSON
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Product.class, name = "SIMPLE"),
    @JsonSubTypes.Type(value = CompositeProduct.class, name = "COMPOSITE")
})
public abstract class ProductComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean isVisibleToClient = true; 

    // Metodo Abstracto que va a ser implementado por los Product y CompositeProduct
    public abstract double getPreparationTime();

    // Precio
    private double price;
}
