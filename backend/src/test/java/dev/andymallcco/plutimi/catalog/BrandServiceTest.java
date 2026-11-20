package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.catalog.dto.BrandResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateBrandRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    private static final Long ID = 3L;

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository, new BrandMapper());
    }

    @Test
    void createSavesBrandWithNormalizedNameAndSlug() {
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = brandService.create(new CreateBrandRequest("  Royal   Canin "));

        assertThat(response.name()).isEqualTo("Royal Canin");
        assertThat(response.slug()).isEqualTo("royal-canin");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(brandRepository.existsByNameIgnoreCase("royal canin")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(new CreateBrandRequest("royal canin")))
                .isInstanceOf(BrandAlreadyExistsException.class)
                .hasMessageContaining("nombre 'royal canin'");
        verify(brandRepository, never()).save(any());
    }

    @Test
    void createRejectsNameWithEquivalentSlug() {
        when(brandRepository.existsBySlug("hill-s")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(new CreateBrandRequest("Hill's")))
                .isInstanceOf(BrandAlreadyExistsException.class)
                .hasMessageContaining("hill-s");
        verify(brandRepository, never()).save(any());
    }

    @Test
    void updateRenamesAndChangesActiveFlag() {
        Brand brand = new Brand("Pro Plan");
        when(brandRepository.findById(ID)).thenReturn(Optional.of(brand));

        BrandResponse response = brandService.update(ID, new UpdateBrandRequest("Purina Pro Plan", false));

        assertThat(response.slug()).isEqualTo("purina-pro-plan");
        assertThat(response.active()).isFalse();
        verify(brandRepository).flush();
    }

    @Test
    void updateRejectsNameUsedByAnotherBrandWithoutModifyingIt() {
        Brand brand = new Brand("Pro Plan");
        when(brandRepository.findById(ID)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Royal Canin", ID)).thenReturn(true);

        assertThatThrownBy(() -> brandService.update(ID, new UpdateBrandRequest("Royal Canin", true)))
                .isInstanceOf(BrandAlreadyExistsException.class);
        assertThat(brand.getName()).isEqualTo("Pro Plan");
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        when(brandRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(ID))
                .isInstanceOf(BrandNotFoundException.class)
                .hasMessage("No existe una marca con id 3");
    }

    @Test
    void deactivateMarksBrandAsInactive() {
        Brand brand = new Brand("Royal Canin");
        when(brandRepository.findById(ID)).thenReturn(Optional.of(brand));

        brandService.deactivate(ID);

        assertThat(brand.isActive()).isFalse();
    }

}
