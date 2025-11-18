package com.restaurante.demo.dto;

import com.restaurante.demo.model.ChefStation;
import com.restaurante.demo.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStatusUpdateDTO {
    private Long itemId;
    private Long orderId;
    private Long chefId; 
    private String chefName;
    private ChefStation station;
    private OrderStatus newStatus;
    private String productName;
    private double estimatedTime;
}
