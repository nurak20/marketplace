package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.MarketplaceCategoryRequest;
import com.oeurn_nurak.marketplace.entity.MarketplaceCategory;
import com.oeurn_nurak.marketplace.entity.MarketplaceCondition;
import com.oeurn_nurak.marketplace.dto.request.MarketplaceConditionRequest;
import com.oeurn_nurak.marketplace.exception.BadRequestException;
import com.oeurn_nurak.marketplace.exception.ConflictException;
import com.oeurn_nurak.marketplace.exception.ResourceNotFoundException;
import com.oeurn_nurak.marketplace.repository.MarketplaceCategoryRepository;
import com.oeurn_nurak.marketplace.repository.MarketplaceConditionRepository;
import com.oeurn_nurak.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketplaceCatalogService {
    private final MarketplaceCategoryRepository categories;
    private final MarketplaceConditionRepository conditions;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public List<MarketplaceCategory> categories() {
        return categories.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceCategory> allCategories() {
        return categories.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceCondition> conditions() {
        return conditions.findByActiveTrueOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceCondition> allConditions() {
        return conditions.findAllByOrderByCodeAsc();
    }

    public MarketplaceCategory createCategory(MarketplaceCategoryRequest request) {
        String name = normalizeName(request.name());
        if (categories.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A category with this name already exists");
        }
        return categories.save(MarketplaceCategory.builder().name(name).active(true).build());
    }

    public MarketplaceCategory renameCategory(Long id, MarketplaceCategoryRequest request) {
        MarketplaceCategory category = requireCategory(id);
        String name = normalizeName(request.name());
        categories.findByNameIgnoreCase(name).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("A category with this name already exists"); });
        products.renameCategory(category.getName(), name);
        category.setName(name);
        return categories.save(category);
    }

    public MarketplaceCategory setCategoryActive(Long id, boolean active) {
        MarketplaceCategory category = requireCategory(id);
        category.setActive(active);
        return categories.save(category);
    }

    public MarketplaceCategory archiveCategory(Long id) {
        return setCategoryActive(id, false);
    }

    public MarketplaceCondition createCondition(MarketplaceConditionRequest request) {
        String code = normalizeConditionCode(request.code());
        if (conditions.existsById(code)) {
            throw new ConflictException("A condition with this name already exists");
        }
        return conditions.save(MarketplaceCondition.builder().code(code).active(true).build());
    }

    public MarketplaceCondition renameCondition(String currentCode, MarketplaceConditionRequest request) {
        MarketplaceCondition condition = requireCondition(currentCode);
        String code = normalizeConditionCode(request.code());
        if (!condition.getCode().equals(code) && conditions.existsById(code)) {
            throw new ConflictException("A condition with this name already exists");
        }
        if (!condition.getCode().equals(code)) {
            products.renameCondition(condition.getCode(), code);
            conditions.delete(condition);
            conditions.flush();
            return conditions.save(MarketplaceCondition.builder().code(code)
                    .active(Boolean.TRUE.equals(condition.getActive())).build());
        }
        return condition;
    }

    public MarketplaceCondition setConditionActive(String code, boolean active) {
        MarketplaceCondition condition = requireCondition(code);
        condition.setActive(active);
        return conditions.save(condition);
    }

    public MarketplaceCondition archiveCondition(String code) {
        return setConditionActive(code, false);
    }

    @Transactional(readOnly = true)
    public String requireActiveCategory(String name) {
        if (name == null || name.isBlank()) return name;
        return categories.findByNameIgnoreCase(name.trim())
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(MarketplaceCategory::getName)
                .orElseThrow(() -> new BadRequestException("Select an active marketplace category"));
    }

    @Transactional(readOnly = true)
    public String requireActiveCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new BadRequestException("Select an active product condition");
        }
        String normalized = normalizeConditionCode(condition);
        return conditions.findById(normalized)
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .map(MarketplaceCondition::getCode)
                .orElseThrow(() -> new BadRequestException("Select an active product condition"));
    }

    private MarketplaceCondition requireCondition(String code) {
        String normalized = normalizeConditionCode(code);
        return conditions.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Product condition not found: " + normalized));
    }

    private String normalizeConditionCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Condition name is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) throw new BadRequestException("Condition name must contain letters or numbers");
        if (normalized.length() > 30) throw new BadRequestException("Condition name must be at most 30 characters");
        return normalized;
    }

    private MarketplaceCategory requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Category name is required");
        return value.trim().replaceAll("\\s+", " ");
    }
}
