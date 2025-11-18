package com.restaurante.demo.dto;

import com.restaurante.demo.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


// Los DTO son clases que nos permiten comunicarnos de manera más limpia con nuestra DB
// En este caso este DTO nos permite conseguir la cola de items y el tiempo total estimado en minutos

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChefQueueDTO {
    private List<OrderItem> itemQueue;
    private double totalEstimatedTimeInMinutes;
}