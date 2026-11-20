package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.money.Money;
import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import dev.andymallcco.plutimi.shared.text.Slugs;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Producto del catálogo. Contiene sus propias reglas: el stock nunca queda negativo, el precio es
 * siempre positivo y todo producto va dirigido al menos a una especie.
 * <p>
 * Las comprobaciones que dependen de datos que la entidad no conoce (SKU único, especies que
 * existen, categoría activa) las hace {@code ProductService} antes de llamar a estos métodos.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Asociaciones JPA permitidas: Category y Brand son del mismo módulo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    private String sku;

    private String name;

    private String slug;

    private String description;

    // Money se guarda en dos columnas de esta tabla; los overrides les dan nombre
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price_amount"))
    @AttributeOverride(name = "currency", column = @Column(name = "price_currency"))
    private Money price;

    private int stock;

    private String imageUrl;

    private boolean active;

    // Bloqueo optimista: Hibernate añade "WHERE version = ?" a cada UPDATE y lo incrementa.
    // Si otro proceso modificó el producto entretanto, el UPDATE no afecta a ninguna fila y
    // se lanza ObjectOptimisticLockingFailureException (409 en GlobalExceptionHandler).
    @Version
    private Long version;

    // Species es de otro módulo: solo se guardan los IDs, en la tabla product_species
    @ElementCollection
    @CollectionTable(name = "product_species", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "species_id")
    private Set<Long> speciesIds = new HashSet<>();

    public Product(String sku, String name, String description, Category category, Brand brand,
                   Money price, int stock, String imageUrl, Set<Long> speciesIds) {
        if (stock < 0) {
            throw new IllegalArgumentException("initial stock must not be negative: " + stock);
        }
        updateDetails(sku, name, description, category, brand, imageUrl);
        changePrice(price);
        changeSpecies(speciesIds);
        this.stock = stock;
        this.active = true;
    }

    /** Datos descriptivos. El precio, el stock y las especies tienen sus propios métodos. */
    public void updateDetails(String sku, String name, String description, Category category,
                              Brand brand, String imageUrl) {
        this.sku = normalizeSku(sku);
        this.name = CatalogNames.normalize(name);
        this.slug = slugOf(this.name, this.sku);
        this.description = blankToNull(description);
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.brand = brand;
        this.imageUrl = blankToNull(imageUrl);
    }

    public void changePrice(Money price) {
        Objects.requireNonNull(price, "price must not be null");
        if (price.isZero()) {
            throw new IllegalArgumentException("price must be positive");
        }
        this.price = price;
    }

    /** Reemplaza las especies. Se modifica el mismo Set para que Hibernate solo borre e inserte las diferencias. */
    public void changeSpecies(Set<Long> speciesIds) {
        Objects.requireNonNull(speciesIds, "speciesIds must not be null");
        if (speciesIds.isEmpty()) {
            throw new IllegalArgumentException("a product must target at least one species");
        }
        this.speciesIds.retainAll(speciesIds);
        this.speciesIds.addAll(speciesIds);
    }

    /**
     * Resta unidades del stock (al vender).
     *
     * @throws InsufficientStockException si no hay unidades suficientes
     */
    public void decreaseStock(int quantity) {
        assertPositiveQuantity(quantity);
        if (quantity > stock) {
            throw new InsufficientStockException(name, stock, quantity);
        }
        this.stock -= quantity;
    }

    /** Suma unidades al stock (reposición o pedido cancelado). */
    public void increaseStock(int quantity) {
        assertPositiveQuantity(quantity);
        this.stock = Math.addExact(stock, quantity);
    }

    /** Ajuste manual del administrador: positivo suma y negativo resta. */
    public void adjustStock(int delta) {
        if (delta > 0) {
            increaseStock(delta);
        } else if (delta < 0) {
            decreaseStock(-delta);
        } else {
            throw new IllegalArgumentException("stock delta must not be zero");
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /** Vista de solo lectura: las especies solo cambian con {@link #changeSpecies(Set)}. */
    public Set<Long> getSpeciesIds() {
        return Collections.unmodifiableSet(speciesIds);
    }

    /** SKU tal como se guardará (sin espacios y en mayúsculas). El service lo usa para comprobar la unicidad. */
    static String normalizeSku(String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return sku.strip().toUpperCase(Locale.ROOT);
    }

    /** Slug a partir de nombre y SKU ya normalizados. El service lo usa para comprobar la unicidad. */
    static String slugOf(String normalizedName, String normalizedSku) {
        return Slugs.of(normalizedName + " " + normalizedSku);
    }

    private static void assertPositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

}
