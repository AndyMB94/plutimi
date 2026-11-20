package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.AdjustStockRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateProductRequest;
import dev.andymallcco.plutimi.catalog.dto.ProductResponse;
import dev.andymallcco.plutimi.catalog.dto.UpdateProductRequest;
import dev.andymallcco.plutimi.shared.money.Money;
import dev.andymallcco.plutimi.species.SpeciesService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de productos. Valida lo que la entidad no puede saber por sí sola (unicidad,
 * categoría y marca activas, especies existentes) y delega las reglas propias en {@link Product}.
 * <p>
 * Categorías y marcas son del mismo módulo, así que se usan sus repositorios directamente.
 * Las especies son de otro módulo: se validan a través de {@link SpeciesService}.
 * El listado con filtros llega con ProductSpecifications.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SpeciesService speciesService;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = Product.normalizeSku(request.sku());
        String name = CatalogNames.normalize(request.name());
        assertSkuAndSlugAvailable(sku, name, null);
        Category category = findActiveCategory(request.categoryId());
        Brand brand = request.brandId() == null ? null : findActiveBrand(request.brandId());
        speciesService.assertActiveSpecies(request.speciesIds());

        Product product = new Product(sku, name, request.description(), category, brand,
                Money.of(request.price(), Money.PEN), request.stock(), request.imageUrl(), request.speciesIds());
        return productMapper.toResponse(productRepository.save(product));
    }

    /**
     * Reemplaza el producto salvo el stock. Todas las comprobaciones se hacen antes de modificar
     * la entidad: con la entidad modificada, una consulta provocaría un auto-flush prematuro.
     */
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findProduct(id);
        String sku = Product.normalizeSku(request.sku());
        String name = CatalogNames.normalize(request.name());
        assertSkuAndSlugAvailable(sku, name, id);
        Category category = resolveCategory(product, request.categoryId(), request.active());
        Brand brand = resolveBrand(product, request.brandId());
        assertNewSpeciesAreActive(product, request.speciesIds());

        product.updateDetails(sku, name, request.description(), category, brand, request.imageUrl());
        product.changePrice(Money.of(request.price(), Money.PEN));
        product.changeSpecies(request.speciesIds());
        if (request.active()) {
            product.activate();
        } else {
            product.deactivate();
        }
        // El flush ejecuta el UPDATE ya: rellena updatedAt e incrementa version antes de responder
        productRepository.flush();
        return productMapper.toResponse(product);
    }

    /**
     * Ajuste manual de stock. Si dos ajustes llegan a la vez, el bloqueo optimista (@Version)
     * hace fallar al segundo con 409 en vez de perder uno de los dos.
     */
    @Transactional
    public ProductResponse adjustStock(Long id, AdjustStockRequest request) {
        Product product = findProduct(id);
        product.adjustStock(request.delta());
        productRepository.flush();
        return productMapper.toResponse(product);
    }

    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    /** Borrado lógico. Es idempotente. */
    @Transactional
    public void deactivate(Long id) {
        findProduct(id).deactivate();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * La categoría debe estar activa si cambia o si el producto queda activo. Así se puede seguir
     * editando un producto inactivo de una categoría desactivada, pero no reactivarlo en ella.
     */
    private Category resolveCategory(Product product, Long categoryId, boolean productWillBeActive) {
        // getId() de un proxy LAZY no dispara ninguna consulta: el id ya está en el proxy
        boolean changes = !Objects.equals(product.getCategory().getId(), categoryId);
        if (changes || productWillBeActive) {
            return findActiveCategory(categoryId);
        }
        return product.getCategory();
    }

    /** La marca solo se exige activa si cambia: desactivar una marca no retira sus productos. */
    private Brand resolveBrand(Product product, Long brandId) {
        if (brandId == null) {
            return null;
        }
        Brand current = product.getBrand();
        if (current != null && Objects.equals(current.getId(), brandId)) {
            return current;
        }
        return findActiveBrand(brandId);
    }

    /** Solo las especies añadidas: las que ya tenía siguen siendo válidas aunque se hayan desactivado. */
    private void assertNewSpeciesAreActive(Product product, Set<Long> requestedSpeciesIds) {
        Set<Long> added = new HashSet<>(requestedSpeciesIds);
        added.removeAll(product.getSpeciesIds());
        if (!added.isEmpty()) {
            speciesService.assertActiveSpecies(added);
        }
    }

    private Category findActiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        if (!category.isActive()) {
            throw new InactiveCategoryException(category.getName());
        }
        return category;
    }

    private Brand findActiveBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
        if (!brand.isActive()) {
            throw new InactiveBrandException(brand.getName());
        }
        return brand;
    }

    /**
     * @param excludedId producto que se está actualizando (no choca consigo mismo); null al crear
     */
    private void assertSkuAndSlugAvailable(String sku, String name, Long excludedId) {
        boolean skuTaken = excludedId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, excludedId);
        if (skuTaken) {
            throw ProductAlreadyExistsException.withSku(sku);
        }

        String slug = Product.slugOf(name, sku);
        boolean slugTaken = excludedId == null
                ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, excludedId);
        if (slugTaken) {
            throw ProductAlreadyExistsException.withSlug(slug);
        }
    }

}
