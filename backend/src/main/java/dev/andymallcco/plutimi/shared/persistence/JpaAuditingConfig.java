package dev.andymallcco.plutimi.shared.persistence;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa la auditoría de JPA (@CreatedDate, @LastModifiedDate) usando el Clock de la aplicación.
 * Está en una clase propia, y no en PlutimiApplication, para que los tests de slice
 * (por ejemplo @WebMvcTest, que no cargan JPA) no intenten activarla.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

}
