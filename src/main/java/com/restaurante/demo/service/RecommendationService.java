package com.restaurante.demo.service;

import com.restaurante.demo.model.CompositeProduct;
import com.restaurante.demo.model.Order;
import com.restaurante.demo.model.ProductComponent;
import com.restaurante.demo.repository.OrderRepository;
import com.restaurante.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    // Simple Cache to avoid running Apriori on every click (Audit recommendation)
    private final Map<Set<Long>, List<ProductComponent>> recommendationCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 60000; // 1 minute cache

    @Autowired
    public RecommendationService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public List<ProductComponent> generateRecommendations(Set<Long> currentItemIds, double minSupport, double minConfidence) {
        if (currentItemIds.isEmpty()) return Collections.emptyList();
        
        // Check cache
        if (System.currentTimeMillis() - lastCacheUpdate < CACHE_TTL && recommendationCache.containsKey(currentItemIds)) {
            return recommendationCache.get(currentItemIds);
        }

        // 1. Conseguir ordenes 
        List<Order> historicalOrders = orderRepository.findAll();
        if (historicalOrders.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Conseguir todos los productos composite que son visibles para el cliente
        Map<Set<Long>, Long> componentToCompositeMap = new HashMap<>();
        productRepository.findAll().stream()
                .filter(p -> p instanceof CompositeProduct && p.isVisibleToClient())
                .forEach(p -> {
                    CompositeProduct composite = (CompositeProduct) p;
                    Set<Long> childIds = composite.getChildren().stream()
                            .map(ProductComponent::getId)
                            .collect(Collectors.toSet());
                    componentToCompositeMap.put(childIds, composite.getId());
                });

        // 3. Construir la database basada en los productos comprados
        List<Set<Long>> transactions = new ArrayList<>();
        for (Order order : historicalOrders) {
            Set<Long> itemsInThisOrder = order.getItems().stream()
                    .map(item -> item.getProduct().getId())
                    .collect(Collectors.toSet());

            Set<Long> compositesInThisOrder = new HashSet<>();
            for (Map.Entry<Set<Long>, Long> entry : componentToCompositeMap.entrySet()) {
                if (itemsInThisOrder.containsAll(entry.getKey())) {
                    compositesInThisOrder.add(entry.getValue());
                }
            }
            // Also include simple items that aren't part of the composite logic if needed
            // For now, following original logic focusing on composites
            if (!compositesInThisOrder.isEmpty()) {
                transactions.add(compositesInThisOrder);
            }
        }
        
        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // --- APRIORI ALGORITMO ---
        Map<Set<Long>, Double> frequentItemsets = findFrequentItemsets(transactions, minSupport);
        List<AssociationRule> rules = generateAssociationRules(frequentItemsets, transactions.size(), minConfidence);

        // --- Filtro de recomendaciones ---
        Set<Long> recommendedProductIds = new HashSet<>();
        for (AssociationRule rule : rules) {
            if (currentItemIds.containsAll(rule.antecedent)) {
                rule.consequent.forEach(recommendedProductIds::add);
            }
        }
        
        recommendedProductIds.removeAll(currentItemIds);

        if (recommendedProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductComponent> results = productRepository.findAllById(recommendedProductIds);
        
        // Update Cache
        recommendationCache.put(currentItemIds, results);
        lastCacheUpdate = System.currentTimeMillis();
        
        return results;
    }

    private Map<Set<Long>, Double> findFrequentItemsets(List<Set<Long>> transactions, double minSupport) {
        int numTransactions = transactions.size();
        Map<Set<Long>, Double> allFrequentItemsets = new HashMap<>();

        Map<Set<Long>, Integer> C1 = new HashMap<>();
        for (Set<Long> transaction : transactions) {
            for (Long item : transaction) {
                Set<Long> itemset = Collections.singleton(item);
                C1.put(itemset, C1.getOrDefault(itemset, 0) + 1);
            }
        }

        Map<Set<Long>, Double> L1 = new HashMap<>();
        for (Map.Entry<Set<Long>, Integer> entry : C1.entrySet()) {
            double support = (double) entry.getValue() / numTransactions;
            if (support >= minSupport) {
                L1.put(entry.getKey(), support);
            }
        }
        allFrequentItemsets.putAll(L1);

        Map<Set<Long>, Double> Lk_1 = L1;
        for (int k = 2; !Lk_1.isEmpty(); k++) {
            Map<Set<Long>, Double> Lk = new HashMap<>();
            Set<Set<Long>> Ck = generateCandidates(Lk_1.keySet(), k);
            Map<Set<Long>, Integer> candidateCounts = new HashMap<>();
            for (Set<Long> transaction : transactions) {
                for (Set<Long> candidate : Ck) {
                    if (transaction.containsAll(candidate)) {
                        candidateCounts.put(candidate, candidateCounts.getOrDefault(candidate, 0) + 1);
                    }
                }
            }

            for (Map.Entry<Set<Long>, Integer> entry : candidateCounts.entrySet()) {
                double support = (double) entry.getValue() / numTransactions;
                if (support >= minSupport) {
                    Lk.put(entry.getKey(), support);
                }
            }
            
            allFrequentItemsets.putAll(Lk);
            Lk_1 = Lk;
        }
        
        return allFrequentItemsets;
    }

    private Set<Set<Long>> generateCandidates(Set<Set<Long>> Lk_1, int k) {
        Set<Set<Long>> candidates = new HashSet<>();
        List<Set<Long>> listLk_1 = new ArrayList<>(Lk_1);
        for (int i = 0; i < listLk_1.size(); i++) {
            for (int j = i + 1; j < listLk_1.size(); j++) {
                Set<Long> union = new HashSet<>(listLk_1.get(i));
                union.addAll(listLk_1.get(j));
                if (union.size() == k) {
                    candidates.add(union);
                }
            }
        }
        return candidates;
    }

    private List<AssociationRule> generateAssociationRules(Map<Set<Long>, Double> frequentItemsets, int numTransactions, double minConfidence) {
        List<AssociationRule> rules = new ArrayList<>();
        for (Map.Entry<Set<Long>, Double> entry : frequentItemsets.entrySet()) {
            Set<Long> itemset = entry.getKey();
            if (itemset.size() > 1) {
                for (Set<Long> subset : getSubsets(itemset)) {
                    if (subset.isEmpty() || subset.equals(itemset)) continue;
                    
                    Set<Long> antecedent = subset;
                    Set<Long> consequent = new HashSet<>(itemset);
                    consequent.removeAll(antecedent);
                    
                    Double itemsetSupportValue = entry.getValue();
                    Double antecedentSupportValue = frequentItemsets.get(antecedent);
                    if (itemsetSupportValue != null && antecedentSupportValue != null && antecedentSupportValue > 0) {
                        double confidence = itemsetSupportValue / antecedentSupportValue;
                        if (confidence >= minConfidence) {
                            rules.add(new AssociationRule(antecedent, consequent, confidence));
                        }
                    }
                }
            }
        }
        return rules;
    }

    private Set<Set<Long>> getSubsets(Set<Long> set) {
        Set<Set<Long>> subsets = new HashSet<>();
        List<Long> elements = new ArrayList<>(set);
        int n = elements.size();
        for (int i = 0; i < (1 << n); i++) {
            Set<Long> subset = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    subset.add(elements.get(j));
                }
            }
            subsets.add(subset);
        }
        return subsets;
    }
    
    private static class AssociationRule {
        final Set<Long> antecedent;
        final Set<Long> consequent;
        final double confidence;

        AssociationRule(Set<Long> antecedent, Set<Long> consequent, double confidence) {
            this.antecedent = antecedent;
            this.consequent = consequent;
            this.confidence = confidence;
        }
    }
}