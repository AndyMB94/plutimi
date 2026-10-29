package dev.andymallcco.plutimi.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import(GlobalExceptionHandlerTest.ErrorTestController.class)
class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-28T19:12:24Z");
    private static final String SECRET = "db-password-123";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void resourceNotFoundReturns404() {
        assertThat(mvc.get().uri("/test/errors/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/resource-not-found",
                          "title": "Recurso no encontrado",
                          "status": 404,
                          "detail": "No existe una muestra con id 7",
                          "instance": "/test/errors/not-found",
                          "timestamp": "2026-10-28T19:12:24Z"
                        }
                        """);
    }

    @Test
    void conflictReturns409() {
        assertThat(mvc.get().uri("/test/errors/conflict"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/conflict",
                          "status": 409,
                          "detail": "Ya existe una muestra con ese nombre"
                        }
                        """);
    }

    @Test
    void businessRuleViolationReturns422() {
        assertThat(mvc.get().uri("/test/errors/business"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/business-rule-violation",
                          "status": 422,
                          "detail": "No hay stock suficiente"
                        }
                        """);
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() {
        MvcTestResult result = mvc.get().uri("/test/errors/unexpected").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "title": "Error interno",
                          "status": 500,
                          "detail": "Ocurrió un error inesperado. Inténtalo de nuevo más tarde."
                        }
                        """);
        assertThat(result).bodyText().doesNotContain(SECRET);
    }

    @Test
    void unknownRouteReturns404InSpanish() {
        assertThat(mvc.get().uri("/does-not-exist"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "title": "Recurso no encontrado",
                          "status": 404,
                          "detail": "La ruta solicitada no existe.",
                          "timestamp": "2026-10-28T19:12:24Z"
                        }
                        """);
    }

    // --- Infraestructura del test ---

    /** Reloj fijo para comprobar el timestamp exacto. Se usa además de la configuración del slice. */
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    /**
     * Controller solo para este test: cada endpoint lanza una excepción.
     * Al estar anidado en una clase de test, el escaneo de componentes lo ignora;
     * solo existe donde se registra con @Import.
     */
    @RestController
    @RequestMapping("/test/errors")
    static class ErrorTestController {

        @GetMapping("/not-found")
        void notFound() {
            throw new SampleNotFoundException(7L);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new SampleConflictException();
        }

        @GetMapping("/business")
        void business() {
            throw new SampleBusinessException();
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("Connection failed using password " + SECRET);
        }

    }

    static class SampleNotFoundException extends ResourceNotFoundException {
        SampleNotFoundException(Long id) {
            super("No existe una muestra con id " + id);
        }
    }

    static class SampleConflictException extends ConflictException {
        SampleConflictException() {
            super("Ya existe una muestra con ese nombre");
        }
    }

    static class SampleBusinessException extends BusinessException {
        SampleBusinessException() {
            super("No hay stock suficiente");
        }
    }

}
