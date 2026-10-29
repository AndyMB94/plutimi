package dev.andymallcco.plutimi.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Respuesta paginada de la API con un formato estable, independiente del JSON interno de {@link Page}.
 * Uso típico en un service: {@code PageResponse.from(products.map(mapper::toSummaryResponse))}.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

}
