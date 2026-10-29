package dev.andymallcco.plutimi.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void copiesPageMetadataAndContent() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void mapsEmptyPage() {
        Page<String> page = Page.empty(PageRequest.of(0, 20));

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void contentIsImmutable() {
        PageResponse<String> response = PageResponse.from(new PageImpl<>(List.of("a")));

        assertThatThrownBy(() -> response.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
