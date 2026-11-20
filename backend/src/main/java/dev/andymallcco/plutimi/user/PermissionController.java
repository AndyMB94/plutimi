package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.PermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Permisos", description = "Catálogo de permisos (solo lectura)")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "Listar permisos",
            description = "Paginado y ordenado por código. Se crean con migraciones, no desde la API.")
    @GetMapping
    public PageResponse<PermissionResponse> list(@ParameterObject @SortDefault(sort = "code") Pageable pageable) {
        return permissionService.list(pageable);
    }

}
