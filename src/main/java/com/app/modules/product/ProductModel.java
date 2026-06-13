package com.app.modules.product;

import com.app.core.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Product entity.
 * Equivalent to Product.php
 */
@Entity
@Table(name = "products")
public class ProductModel extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 3, message = "Name must be at least 3 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, message = "SKU must be at least 3 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    private String category;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(columnDefinition = "integer default 0")
    private Integer stock = 0;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "id_user", length = 40)
    @com.fasterxml.jackson.annotation.JsonProperty("id_user")
    private String idUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.app.modules.user.UserModel user;

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public com.app.modules.user.UserModel getUser() {
        return user;
    }

    public void setUser(com.app.modules.user.UserModel user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
