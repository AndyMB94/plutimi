package dev.andymallcco.plutimi.shared.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Orígenes (frontends) autorizados a llamar a la API desde el navegador.
 * Se lee de las propiedades {@code plutimi.cors.*}.
 */
@Validated
@ConfigurationProperties(prefix = "plutimi.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
