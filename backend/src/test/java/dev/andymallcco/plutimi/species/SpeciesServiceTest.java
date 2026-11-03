package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.species.dto.BreedResponse;
import dev.andymallcco.plutimi.species.dto.CreateBreedRequest;
import dev.andymallcco.plutimi.species.dto.CreateSpeciesRequest;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import dev.andymallcco.plutimi.species.dto.UpdateBreedRequest;
import dev.andymallcco.plutimi.species.dto.UpdateSpeciesRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitario: los repositorios son mocks de Mockito, sin Spring ni base de datos.
 * Las consultas reales se prueban en el test de integración.
 */
@ExtendWith(MockitoExtension.class)
class SpeciesServiceTest {

    private static final Long DOG_ID = 1L;
    private static final Long CAT_ID = 2L;
    private static final Long BREED_ID = 10L;

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private BreedRepository breedRepository;

    private SpeciesService speciesService;

    private Species dog;

    @BeforeEach
    void setUp() {
        speciesService = new SpeciesService(speciesRepository, breedRepository, new SpeciesMapper(), new BreedMapper());
        dog = persistedSpecies(DOG_ID, "DOG", "Perro");
    }

    // --- createSpecies ---

    @Test
    void createSpeciesSavesItWithNormalizedName() {
        when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpeciesResponse response = speciesService.createSpecies(new CreateSpeciesRequest("GUINEA_PIG", "  Cuy  "));

        assertThat(response.code()).isEqualTo("GUINEA_PIG");
        assertThat(response.name()).isEqualTo("Cuy");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createSpeciesRejectsDuplicateCode() {
        when(speciesRepository.existsByCode("DOG")).thenReturn(true);

        assertThatThrownBy(() -> speciesService.createSpecies(new CreateSpeciesRequest("DOG", "Can")))
                .isInstanceOf(SpeciesAlreadyExistsException.class)
                .hasMessageContaining("código 'DOG'");
        verify(speciesRepository, never()).save(any());
    }

    @Test
    void createSpeciesRejectsDuplicateNameIgnoringCase() {
        when(speciesRepository.existsByNameIgnoreCase("perro")).thenReturn(true);

        assertThatThrownBy(() -> speciesService.createSpecies(new CreateSpeciesRequest("CANINE", "perro")))
                .isInstanceOf(SpeciesAlreadyExistsException.class)
                .hasMessageContaining("nombre 'perro'");
        verify(speciesRepository, never()).save(any());
    }

    // --- updateSpecies ---

    @Test
    void updateSpeciesChangesNameAndActiveFlag() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));

        SpeciesResponse response = speciesService.updateSpecies(DOG_ID, new UpdateSpeciesRequest("Perros", false));

        assertThat(response.name()).isEqualTo("Perros");
        assertThat(response.code()).isEqualTo("DOG");
        assertThat(response.active()).isFalse();
        verify(speciesRepository).flush();
    }

    @Test
    void updateSpeciesRejectsNameUsedByAnotherSpecies() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(speciesRepository.existsByNameIgnoreCaseAndIdNot("Gato", DOG_ID)).thenReturn(true);

        assertThatThrownBy(() -> speciesService.updateSpecies(DOG_ID, new UpdateSpeciesRequest("Gato", true)))
                .isInstanceOf(SpeciesAlreadyExistsException.class);
        // La entidad no se modificó: la validación ocurre antes del update
        assertThat(dog.getName()).isEqualTo("Perro");
    }

    // --- getSpecies / listSpecies / deactivateSpecies ---

    @Test
    void getSpeciesThrowsNotFoundForUnknownId() {
        when(speciesRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.getSpecies(99L))
                .isInstanceOf(SpeciesNotFoundException.class)
                .hasMessage("No existe una especie con id 99");
    }

    @Test
    void listSpeciesReturnsOnlyActiveByDefault() {
        Pageable pageable = PageRequest.of(0, 100);
        when(speciesRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(dog), pageable, 1));

        PageResponse<SpeciesResponse> page = speciesService.listSpecies(false, pageable);

        assertThat(page.content()).extracting(SpeciesResponse::code).containsExactly("DOG");
        verify(speciesRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void deactivateSpeciesDoesNotDeactivateItsBreeds() {
        Breed beagle = dog.newBreed("Beagle");
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));

        speciesService.deactivateSpecies(DOG_ID);

        assertThat(dog.isActive()).isFalse();
        assertThat(beagle.isActive()).isTrue();
    }

    // --- listBreeds ---

    @Test
    void listBreedsThrowsNotFoundForUnknownSpecies() {
        when(speciesRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> speciesService.listBreeds(99L, false, PageRequest.of(0, 100)))
                .isInstanceOf(SpeciesNotFoundException.class);
    }

    @Test
    void listBreedsReturnsOnlyActiveByDefault() {
        Pageable pageable = PageRequest.of(0, 100);
        when(speciesRepository.existsById(DOG_ID)).thenReturn(true);
        when(breedRepository.findBySpeciesIdAndActiveTrue(DOG_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(dog.newBreed("Beagle")), pageable, 1));

        PageResponse<BreedResponse> page = speciesService.listBreeds(DOG_ID, false, pageable);

        assertThat(page.content()).extracting(BreedResponse::name).containsExactly("Beagle");
        assertThat(page.content()).extracting(BreedResponse::speciesId).containsExactly(DOG_ID);
        verify(breedRepository, never()).findBySpeciesId(anyLong(), any());
    }

    // --- createBreed ---

    @Test
    void createBreedSavesItInTheSpecies() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(breedRepository.save(any(Breed.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BreedResponse response = speciesService.createBreed(DOG_ID, new CreateBreedRequest(" Border   Collie "));

        assertThat(response.name()).isEqualTo("Border Collie");
        assertThat(response.speciesId()).isEqualTo(DOG_ID);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createBreedRejectsDuplicateNameInTheSameSpecies() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(breedRepository.existsBySpeciesIdAndNameIgnoreCase(DOG_ID, "beagle")).thenReturn(true);

        assertThatThrownBy(() -> speciesService.createBreed(DOG_ID, new CreateBreedRequest("beagle")))
                .isInstanceOf(BreedAlreadyExistsException.class)
                .hasMessage("Ya existe la raza 'beagle' en la especie 'Perro'");
        verify(breedRepository, never()).save(any());
    }

    @Test
    void createBreedRejectsInactiveSpecies() {
        dog.deactivate();
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));

        assertThatThrownBy(() -> speciesService.createBreed(DOG_ID, new CreateBreedRequest("Beagle")))
                .isInstanceOf(InactiveSpeciesException.class);
        verify(breedRepository, never()).existsBySpeciesIdAndNameIgnoreCase(anyLong(), anyString());
        verify(breedRepository, never()).save(any());
    }

    // --- updateBreed / deactivateBreed ---

    @Test
    void updateBreedChangesNameAndActiveFlag() {
        Breed breed = persistedBreed(BREED_ID, dog, "Labrador");
        when(breedRepository.findById(BREED_ID)).thenReturn(Optional.of(breed));

        BreedResponse response = speciesService.updateBreed(BREED_ID, new UpdateBreedRequest("Labrador Retriever", false));

        assertThat(response.name()).isEqualTo("Labrador Retriever");
        assertThat(response.active()).isFalse();
        verify(breedRepository).flush();
    }

    @Test
    void updateBreedRejectsNameUsedByAnotherBreedOfTheSameSpecies() {
        Breed breed = persistedBreed(BREED_ID, dog, "Labrador");
        when(breedRepository.findById(BREED_ID)).thenReturn(Optional.of(breed));
        when(breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(DOG_ID, "Beagle", BREED_ID)).thenReturn(true);

        assertThatThrownBy(() -> speciesService.updateBreed(BREED_ID, new UpdateBreedRequest("Beagle", true)))
                .isInstanceOf(BreedAlreadyExistsException.class);
        assertThat(breed.getName()).isEqualTo("Labrador");
    }

    @Test
    void deactivateBreedThrowsNotFoundForUnknownId() {
        when(breedRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.deactivateBreed(99L))
                .isInstanceOf(BreedNotFoundException.class)
                .hasMessage("No existe una raza con id 99");
    }

    // --- assertBreedBelongsToSpecies ---

    @Test
    void assertBreedBelongsToSpeciesAcceptsActiveBreedOfThatSpecies() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(breedRepository.findById(BREED_ID)).thenReturn(Optional.of(persistedBreed(BREED_ID, dog, "Beagle")));

        assertThatCode(() -> speciesService.assertBreedBelongsToSpecies(BREED_ID, DOG_ID)).doesNotThrowAnyException();
    }

    @Test
    void assertBreedBelongsToSpeciesRejectsBreedOfAnotherSpecies() {
        Species cat = persistedSpecies(CAT_ID, "CAT", "Gato");
        when(speciesRepository.findById(CAT_ID)).thenReturn(Optional.of(cat));
        when(breedRepository.findById(BREED_ID)).thenReturn(Optional.of(persistedBreed(BREED_ID, dog, "Labrador")));

        assertThatThrownBy(() -> speciesService.assertBreedBelongsToSpecies(BREED_ID, CAT_ID))
                .isInstanceOf(BreedSpeciesMismatchException.class)
                .hasMessage("La raza 'Labrador' no pertenece a la especie 'Gato'");
    }

    @Test
    void assertBreedBelongsToSpeciesRejectsInactiveSpecies() {
        dog.deactivate();
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));

        assertThatThrownBy(() -> speciesService.assertBreedBelongsToSpecies(BREED_ID, DOG_ID))
                .isInstanceOf(InactiveSpeciesException.class);
    }

    @Test
    void assertBreedBelongsToSpeciesRejectsInactiveBreed() {
        Breed breed = persistedBreed(BREED_ID, dog, "Beagle");
        breed.deactivate();
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(breedRepository.findById(BREED_ID)).thenReturn(Optional.of(breed));

        assertThatThrownBy(() -> speciesService.assertBreedBelongsToSpecies(BREED_ID, DOG_ID))
                .isInstanceOf(InactiveBreedException.class);
    }

    @Test
    void assertBreedBelongsToSpeciesThrowsNotFoundForUnknownBreed() {
        when(speciesRepository.findById(DOG_ID)).thenReturn(Optional.of(dog));
        when(breedRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.assertBreedBelongsToSpecies(99L, DOG_ID))
                .isInstanceOf(BreedNotFoundException.class);
    }

    // Simulan entidades ya persistidas: el id lo asigna la base de datos

    private static Species persistedSpecies(Long id, String code, String name) {
        Species species = new Species(code, name);
        ReflectionTestUtils.setField(species, "id", id);
        return species;
    }

    private static Breed persistedBreed(Long id, Species species, String name) {
        Breed breed = species.newBreed(name);
        ReflectionTestUtils.setField(breed, "id", id);
        return breed;
    }

}
