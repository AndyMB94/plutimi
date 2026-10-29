package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.CategoryResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateCategoryRequest;
import dev.andymallcco.plutimi.shared.text.Slugs;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de categorías. Es la API pública del módulo para categorías:
 * recibe y devuelve DTOs, nunca entidades.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = Category.normalizeName(request.name());
        assertNameAndSlugAvailable(name, null);

        Category category = categoryRepository.save(new Category(name, request.description()));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findCategory(id);
        String name = Category.normalizeName(request.name());
        // Comprobar ANTES de modificar la entidad: si estuviera modificada, la consulta provocaría
        // un auto-flush y el UPDATE chocaría con el índice único antes de nuestra validación.
        assertNameAndSlugAvailable(name, id);

        category.update(name, request.description());
        if (request.active()) {
            category.activate();
        } else {
            category.deactivate();
        }
        // Sin save(): la entidad está gestionada y Hibernate detecta los cambios (dirty checking).
        // El flush ejecuta ya el UPDATE para que la auditoría rellene updatedAt antes de responder.
        categoryRepository.flush();
        return categoryMapper.toResponse(category);
    }

    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    public PageResponse<CategoryResponse> list(boolean includeInactive, Pageable pageable) {
        Page<Category> categories = includeInactive
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByActiveTrue(pageable);
        return PageResponse.from(categories.map(categoryMapper::toResponse));
    }

    /** Borrado lógico. Es idempotente: desactivar una categoría ya inactiva no falla. */
    @Transactional
    public void deactivate(Long id) {
        findCategory(id).deactivate();
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    /**
     * @param excludedId categoría que se está actualizando (no choca consigo misma); null al crear
     */
    private void assertNameAndSlugAvailable(String name, Long excludedId) {
        boolean nameTaken = excludedId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
        if (nameTaken) {
            throw CategoryAlreadyExistsException.withName(name);
        }

        String slug = Slugs.of(name);
        boolean slugTaken = excludedId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, excludedId);
        if (slugTaken) {
            throw CategoryAlreadyExistsException.withSlug(slug);
        }
    }

}
