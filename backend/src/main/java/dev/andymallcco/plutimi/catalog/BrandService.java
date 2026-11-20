package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.BrandResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateBrandRequest;
import dev.andymallcco.plutimi.shared.text.Slugs;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de marcas. Desactivar una marca se permite aunque tenga productos activos:
 * la marca es opcional en el producto y este sigue siendo vendible.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional
    public BrandResponse create(CreateBrandRequest request) {
        String name = CatalogNames.normalize(request.name());
        assertNameAndSlugAvailable(name, null);

        Brand brand = brandRepository.save(new Brand(name));
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse update(Long id, UpdateBrandRequest request) {
        Brand brand = findBrand(id);
        String name = CatalogNames.normalize(request.name());
        // Antes de modificar la entidad, para evitar el auto-flush (ver CategoryService.update)
        assertNameAndSlugAvailable(name, id);

        brand.rename(name);
        if (request.active()) {
            brand.activate();
        } else {
            brand.deactivate();
        }
        brandRepository.flush();
        return brandMapper.toResponse(brand);
    }

    public BrandResponse getById(Long id) {
        return brandMapper.toResponse(findBrand(id));
    }

    public PageResponse<BrandResponse> list(boolean includeInactive, Pageable pageable) {
        Page<Brand> brands = includeInactive
                ? brandRepository.findAll(pageable)
                : brandRepository.findByActiveTrue(pageable);
        return PageResponse.from(brands.map(brandMapper::toResponse));
    }

    /** Borrado lógico. Es idempotente. */
    @Transactional
    public void deactivate(Long id) {
        findBrand(id).deactivate();
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
    }

    /**
     * @param excludedId marca que se está actualizando (no choca consigo misma); null al crear
     */
    private void assertNameAndSlugAvailable(String name, Long excludedId) {
        boolean nameTaken = excludedId == null
                ? brandRepository.existsByNameIgnoreCase(name)
                : brandRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
        if (nameTaken) {
            throw BrandAlreadyExistsException.withName(name);
        }

        String slug = Slugs.of(name);
        boolean slugTaken = excludedId == null
                ? brandRepository.existsBySlug(slug)
                : brandRepository.existsBySlugAndIdNot(slug, excludedId);
        if (slugTaken) {
            throw BrandAlreadyExistsException.withSlug(slug);
        }
    }

}
