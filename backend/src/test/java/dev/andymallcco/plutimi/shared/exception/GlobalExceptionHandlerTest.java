package dev.andymallcco.plutimi.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slice web aislado: {@code controllers} limita los controllers cargados al de prueba, así los
 * controllers reales de los módulos (que necesitan sus services) no entran en el contexto.
 * El @ControllerAdvice y los WebMvcConfigurer se siguen cargando.
 * El @Import es necesario porque el escaneo ignora las clases anidadas en un test.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErrorTestController.class)
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

    @Test
    void invalidRequestBodyReturns400WithFieldErrors() {
        assertThat(mvc.post().uri("/test/errors/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "", "price": null }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/validation-error",
                          "title": "Datos inválidos",
                          "status": 400,
                          "detail": "La petición contiene 2 errores de validación",
                          "errors": [
                            { "field": "name",  "message": "El nombre es obligatorio" },
                            { "field": "price", "message": "no debe ser nulo" }
                          ]
                        }
                        """);
    }

    @Test
    void invalidPathVariableReturns400WithParameterError() {
        assertThat(mvc.get().uri("/test/errors/validation/0"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/validation-error",
                          "status": 400,
                          "detail": "La petición contiene 1 error de validación",
                          "errors": [
                            { "field": "id", "message": "El id debe ser positivo" }
                          ]
                        }
                        """);
    }

    @Test
    void malformedJsonReturns400InSpanish() {
        MvcTestResult result = mvc.post().uri("/test/errors/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": ")
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "title": "Petición mal formada",
                          "status": 400,
                          "detail": "El cuerpo de la petición falta, no es un JSON válido o contiene valores con un tipo incorrecto."
                        }
                        """);
        assertThat(result).bodyText().doesNotContain("SampleRequest");
    }

    @Test
    void optimisticLockingFailureReturns409() {
        assertThat(mvc.get().uri("/test/errors/optimistic-lock"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/concurrent-modification",
                          "title": "Modificación concurrente",
                          "status": 409
                        }
                        """);
    }

    @Test
    void dataIntegrityViolationReturns409WithoutConstraintDetails() {
        MvcTestResult result = mvc.get().uri("/test/errors/data-integrity").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/data-conflict",
                          "title": "Conflicto de datos",
                          "status": 409,
                          "detail": "La operación entra en conflicto con datos existentes."
                        }
                        """);
        assertThat(result).bodyText().doesNotContain("uk_samples_name");
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

        @GetMapping("/optimistic-lock")
        void optimisticLock() {
            throw new OptimisticLockingFailureException("Row was updated by another transaction");
        }

        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"uk_samples_name\"");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/validation/{id}")
        void pathValidation(@PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        }

    }

    /** {@code price} usa el mensaje por defecto de @NotNull para comprobar que sale en español. */
    public record SampleRequest(
            @NotBlank(message = "El nombre es obligatorio") String name,
            @NotNull BigDecimal price) {
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
