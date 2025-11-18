package com.restaurante.demo;

import com.restaurante.demo.model.*;
import com.restaurante.demo.repository.ChefRepository;
import com.restaurante.demo.repository.OrderRepository;
import com.restaurante.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseAlgorithmTest {

    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected ChefRepository chefRepository;
    @Autowired
    protected OrderRepository orderRepository;

    protected ProductComponent burgerPatty, bun, cheese, lettuce, fries, soda;
    protected CompositeProduct cheeseburger, mealDeal;

    /**
     * Configura un conjunto estándar de productos y chefs antes de cada prueba.
     */
    protected void setupTestData() {
        // Limpiar la base de datos para un inicio limpio
        orderRepository.deleteAll();
        productRepository.deleteAll();
        chefRepository.deleteAll();

        // --- Crear Productos Simples (Ingredientes) ---
        burgerPatty = createSimpleProduct("Burger Patty", 2.0, 5.0, ChefStation.GRILL_STATION, null);
        bun = createSimpleProduct("Bun", 0.50, 1.0, ChefStation.SOUS_CHEF_STATION, null);
        cheese = createSimpleProduct("Cheese", 0.75, 0.5, ChefStation.SOUS_CHEF_STATION, null);
        lettuce = createSimpleProduct("Lettuce", 0.25, 0.5, ChefStation.SOUS_CHEF_STATION, null);

        // --- Crear un Producto Compuesto (Cheeseburger) ---
        cheeseburger = new CompositeProduct();
        cheeseburger.setName("Cheeseburger");
        cheeseburger.setPrice(5.50);
        cheeseburger.setVisibleToClient(true);
        cheeseburger.setChildren(List.of(burgerPatty, bun, cheese, lettuce));
        productRepository.save(cheeseburger);

        // --- Crear Ítems de Menú Independientes ---
        fries = createSimpleProduct("Fries", 2.50, 3.0, ChefStation.SOUS_CHEF_STATION, null);
        soda = createSimpleProduct("Soda", 1.50, 0.5, ChefStation.PASTRY_STATION, null);

        // --- Crear un Producto Compuesto más grande (Combo) ---
        mealDeal = new CompositeProduct();
        mealDeal.setName("Meal Deal");
        mealDeal.setPrice(8.50);
        mealDeal.setVisibleToClient(true);
        mealDeal.setChildren(List.of(cheeseburger, fries, soda));
        productRepository.save(mealDeal);
        
        // --- Crear Chefs para cada estación ---
        createChef("Gordon", "grill_chef", ChefStation.GRILL_STATION, 1.2);
        createChef("Jamie", "sous_chef", ChefStation.SOUS_CHEF_STATION, 1.0);
        createChef("Nigella", "pastry_chef", ChefStation.PASTRY_STATION, 1.1);
    }

    protected Product createSimpleProduct(String name, double price, double prepTime, ChefStation station, Long prerequisiteId) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setPreparationTime(prepTime);
        p.setRequiredStation(station);
        p.setPrerequisiteProductId(prerequisiteId);
        p.setVisibleToClient(false); // Por defecto es un ingrediente, no visible al cliente
        return productRepository.save(p);
    }

    protected Chef createChef(String nombre, String username, ChefStation station, double efficiency) {
        Chef chef = new Chef();
        chef.setNombre(nombre);
        chef.setUsername(username);
        chef.setPassword("password");
        chef.setStation(station);
        chef.setEfficiency(efficiency);
        return chefRepository.save(chef);
    }
}