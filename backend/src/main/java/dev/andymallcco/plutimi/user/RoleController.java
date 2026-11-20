package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.CreateRoleRequest;
import dev.andymallcco.plutimi.user.dto.ReplaceRolePermissionsRequest;
import dev.andymallcco.plutimi.user.dto.RoleResponse;
import dev.andymallcco.plutimi.user.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Roles", description = "Roles y sus permisos (RBAC)")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Listar roles", description = "Paginado y ordenado por código, con sus permisos.")
    @GetMapping
    public PageResponse<RoleResponse> list(@ParameterObject @SortDefault(sort = "code") Pageable pageable) {
        return roleService.list(pageable);
    }

    @Operation(summary = "Obtener un rol", description = "Incluye sus permisos.")
    @GetMapping("/{id}")
    public RoleResponse getById(@PathVariable @Positive Long id) {
        return roleService.getById(id);
    }

    @Operation(summary = "Crear un rol",
            description = "Se crea sin permisos; se asignan con PUT /roles/{id}/permissions. El código no se puede cambiar después.")
    @ApiResponse(responseCode = "201", description = "Rol creado",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL del rol creado",
                    schema = @Schema(type = "string", format = "uri")))
    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse created = roleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Cambiar nombre y descripción de un rol", description = "El código no cambia nunca.")
    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable @Positive Long id,
                               @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.update(id, request);
    }

    @Operation(summary = "Reemplazar los permisos de un rol",
            description = "Los permisos que no aparecen se quitan. ADMIN siempre conserva role:manage y user:manage.")
    @PutMapping("/{id}/permissions")
    public RoleResponse replacePermissions(@PathVariable @Positive Long id,
                                           @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        return roleService.replacePermissions(id, request);
    }

    @Operation(summary = "Borrar un rol", description = "Los roles de sistema (CUSTOMER, VET, ADMIN) y los que tienen usuarios no se pueden borrar.")
    @ApiResponse(responseCode = "204", description = "Rol borrado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        roleService.delete(id);
    }

}
