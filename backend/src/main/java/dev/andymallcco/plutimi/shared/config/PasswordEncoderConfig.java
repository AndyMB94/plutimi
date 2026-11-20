package dev.andymallcco.plutimi.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hash de contraseñas con BCrypt. Se inyecta la interfaz {@link PasswordEncoder}, así que cambiar
 * de algoritmo solo toca este bean.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Coste 10 (por defecto): unos 100 ms por hash, lento a propósito contra fuerza bruta
        return new BCryptPasswordEncoder();
    }

}
