package com.restaurante.demo.service;

import com.restaurante.demo.model.CompositeProduct;
import com.restaurante.demo.model.Product;
import com.restaurante.demo.model.ProductComponent;
import com.restaurante.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductComponent createProduct(ProductComponent product) {
        if (product.getId() != null) {
            throw new IllegalArgumentException("Cannot create a product with an existing ID. Use update instead.");
        }
        if (product instanceof CompositeProduct) {
            CompositeProduct composite = (CompositeProduct) product;
            if (composite.getChildren() != null && !composite.getChildren().isEmpty()) {
                List<Long> childIds = composite.getChildren().stream()
                        .map(ProductComponent::getId)
                        .collect(Collectors.toList());
                List<ProductComponent> managedChildren = productRepository.findAllById(childIds);
                composite.setChildren(managedChildren);
            }
        }
        return productRepository.save(product);
    }

    @Transactional
    public ProductComponent updateProduct(Long id, ProductComponent productDetails) {
        ProductComponent existingProduct = getProductById(id);
        existingProduct.setName(productDetails.getName());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setVisibleToClient(productDetails.isVisibleToClient());

        if (existingProduct instanceof Product && productDetails instanceof Product) {
            Product existingSimple = (Product) existingProduct;
            Product detailsSimple = (Product) productDetails;
            existingSimple.setPreparationTime(detailsSimple.getPreparationTime());
            existingSimple.setRequiredStation(detailsSimple.getRequiredStation());
            existingSimple.setPrerequisiteProductId(detailsSimple.getPrerequisiteProductId());
        } else if (existingProduct instanceof CompositeProduct && productDetails instanceof CompositeProduct) {
             CompositeProduct existingComposite = (CompositeProduct) existingProduct;
             CompositeProduct detailsComposite = (CompositeProduct) productDetails;
             if (detailsComposite.getChildren() != null) {
                 List<Long> childIds = detailsComposite.getChildren().stream().map(ProductComponent::getId).collect(Collectors.toList());
                 List<ProductComponent> children = productRepository.findAllById(childIds);
                 existingComposite.setChildren(children);
             }
        }
        return productRepository.save(existingProduct);
    }
    
    // --- GETTER METHODS ---

    /**
     * This now calls the corrected, explicit @Query method from the repository.
     */
    public List<ProductComponent> getVisibleProducts() {
        return productRepository.findAllVisibleToClient();
    }

    public List<ProductComponent> getAllProducts() {
        return productRepository.findAll();
    }
    
    public ProductComponent getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}