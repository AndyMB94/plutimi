package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.species.dto.BreedResponse;
import dev.andymallcco.plutimi.species.dto.CreateBreedRequest;
import dev.andymallcco.plutimi.species.dto.CreateSpeciesRequest;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import dev.andymallcco.plutimi.species.dto.UpdateBreedRequest;
import dev.andymallcco.plutimi.species.dto.UpdateSpeciesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de especies y razas. Es la API pública del módulo: recibe y devuelve DTOs,
 * nunca entidades. Los demás módulos validan aquí las especies y razas que referencian.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final SpeciesMapper speciesMapper;
    private final BreedMapper breedMapper;

    // --- Especies ---

    public PageResponse<SpeciesResponse> listSpecies(boolean includeInactive, Pageable pageable) {
        Page<Species> species = includeInactive
                ? speciesRepository.findAll(pageable)
                : speciesRepository.findByActiveTrue(pageable);
        return PageResponse.from(species.map(speciesMapper::toResponse));
    }

    public SpeciesResponse getSpecies(Long id) {
        return speciesMapper.toResponse(findSpecies(id));
    }

    @Transactional
    public SpeciesResponse createSpecies(CreateSpeciesRequest request) {
        if (speciesRepository.existsByCode(request.code())) {
            throw SpeciesAlreadyExistsException.withCode(request.code());
        }
        String name = Species.normalizeName(request.name());
        if (speciesRepository.existsByNameIgnoreCase(name)) {
            throw SpeciesAlreadyExistsException.withName(name);
        }

        Species species = speciesRepository.save(new Species(request.code(), name));
        return speciesMapper.toResponse(species);
    }

    @Transactional
    public SpeciesResponse updateSpecies(Long id, UpdateSpeciesRequest request) {
        Species species = findSpecies(id);
        String name = Species.normalizeName(request.name());
        // Comprobar ANTES de modificar la entidad, para evitar el auto-flush (ver CategoryService.update)
        if (speciesRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw SpeciesAlreadyExistsException.withName(name);
        }

        species.update(name);
        if (request.active()) {
            species.activate();
        } else {
            species.deactivate();
        }
        // El flush ejecuta ya el UPDATE para que la auditoría rellene updatedAt antes de responder
        speciesRepository.flush();
        return speciesMapper.toResponse(species);
    }

    /**
     * Borrado lógico. Es idempotente. Las razas no se desactivan en cascada: si la especie se
     * reactiva, recupera sus razas tal como estaban.
     */
    @Transactional
    public void deactivateSpecies(Long id) {
        findSpecies(id).deactivate();
    }

    // --- Razas ---

    public PageResponse<BreedResponse> listBreeds(Long speciesId, boolean includeInactive, Pageable pageable) {
        if (!speciesRepository.existsById(speciesId)) {
            throw new SpeciesNotFoundException(speciesId);
        }
        Page<Breed> breeds = includeInactive
                ? breedRepository.findBySpeciesId(speciesId, pageable)
                : breedRepository.findBySpeciesIdAndActiveTrue(speciesId, pageable);
        return PageResponse.from(breeds.map(breedMapper::toResponse));
    }

    public BreedResponse getBreed(Long id) {
        return breedMapper.toResponse(findBreed(id));
    }

    @Transactional
    public BreedResponse createBreed(Long speciesId, CreateBreedRequest request) {
        Species species = findSpecies(speciesId);
        // newBreed aplica primero la regla de la especie activa (422). La raza aún no está
        // gestionada por JPA, así que la consulta de unicidad no provoca un auto-flush.
        Breed breed = species.newBreed(request.name());
        if (breedRepository.existsBySpeciesIdAndNameIgnoreCase(speciesId, breed.getName())) {
            throw new BreedAlreadyExistsException(breed.getName(), species.getName());
        }

        return breedMapper.toResponse(breedRepository.save(breed));
    }

    @Transactional
    public BreedResponse updateBreed(Long id, UpdateBreedRequest request) {
        Breed breed = findBreed(id);
        Species species = breed.getSpecies();
        String name = Species.normalizeName(request.name());
        if (breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(species.getId(), name, id)) {
            throw new BreedAlreadyExistsException(name, species.getName());
        }

        breed.update(name);
        if (request.active()) {
            breed.activate();
        } else {
            breed.deactivate();
        }
        breedRepository.flush();
        return breedMapper.toResponse(breed);
    }

    /** Borrado lógico. Es idempotente. */
    @Transactional
    public void deactivateBreed(Long id) {
        findBreed(id).deactivate();
    }

    /**
     * Para otros módulos (mascotas): comprueba que la especie y la raza existen, están activas y
     * que la raza pertenece a la especie. La FK compuesta de pets lo garantiza también en la base
     * de datos, pero aquí se valida antes para devolver un error claro.
     *
     * @throws SpeciesNotFoundException      si la especie no existe (404)
     * @throws BreedNotFoundException        si la raza no existe (404)
     * @throws InactiveSpeciesException      si la especie está desactivada (422)
     * @throws BreedSpeciesMismatchException si la raza es de otra especie (422)
     * @throws InactiveBreedException        si la raza está desactivada (422)
     */
    public void assertBreedBelongsToSpecies(Long breedId, Long speciesId) {
        Species species = findSpecies(speciesId);
        if (!species.isActive()) {
            throw new InactiveSpeciesException(species.getName());
        }
        Breed breed = findBreed(breedId);
        if (!breed.getSpecies().getId().equals(speciesId)) {
            throw new BreedSpeciesMismatchException(breed.getName(), species.getName());
        }
        if (!breed.isActive()) {
            throw new InactiveBreedException(breed.getName());
        }
    }

    private Species findSpecies(Long id) {
        return speciesRepository.findById(id)
                .orElseThrow(() -> new SpeciesNotFoundException(id));
    }

    private Breed findBreed(Long id) {
        return breedRepository.findById(id)
                .orElseThrow(() -> new BreedNotFoundException(id));
    }

}
