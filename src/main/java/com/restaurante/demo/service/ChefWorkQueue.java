package com.restaurante.demo.service;

import com.restaurante.demo.model.OrderItem;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ChefWorkQueue {
    private final BlockingQueue<OrderItem> itemQueue = new LinkedBlockingQueue<>();
    // Cada Cola de Chef va a ser un Queue thread
    private final AtomicLong totalEstimatedTime = new AtomicLong(0);

    /**
     * Anade un item e incrementa el tiempo total estimado
     * @param item The order item to add.
     */
    public void addItem(OrderItem item) {
        long itemPrepTime = (long) (item.getPreparationTime() * 100);
        if (this.itemQueue.add(item)) {
            totalEstimatedTime.addAndGet(itemPrepTime);
        }
    }

    /**
     * Elimina un item y decrementa el tiempo total estimado
     * @return The removed order item.
     */
    public OrderItem takeItem() throws InterruptedException {
        OrderItem item = this.itemQueue.take();
        long itemPrepTime = (long) (item.getPreparationTime() * 100);
        totalEstimatedTime.addAndGet(-itemPrepTime);
        return item;
    }

    public double getTotalEstimatedTimeInMinutes() {
        return totalEstimatedTime.get() / 100.0;
    }
}
