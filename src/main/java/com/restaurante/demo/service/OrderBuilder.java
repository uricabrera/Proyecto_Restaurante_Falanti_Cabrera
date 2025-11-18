package com.restaurante.demo.service;

import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.OrderItem;
import com.restaurante.demo.model.Product;

public class OrderBuilder {
    private Order order;

    public OrderBuilder create() {
        this.order = new Order();
        return this;
    }

    public OrderBuilder withItem(Product product, int quantity) {
        if (this.order == null) {
            throw new IllegalStateException("Must call create() before adding items.");
        }
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setOrder(this.order);
        this.order.getItems().add(item);
        return this;
    }

    public Order build() {
        Order builtOrder = this.order;
        this.order = null; // Reset for next build
        return builtOrder;
    }
}
