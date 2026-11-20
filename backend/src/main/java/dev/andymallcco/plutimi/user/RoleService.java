package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.CreateRoleRequest;
import dev.andymallcco.plutimi.user.dto.ReplaceRolePermissionsRequest;
import dev.andymallcco.plutimi.user.dto.RoleResponse;
import dev.andymallcco.plutimi.user.dto.UpdateRoleRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de roles. Las reglas propias del rol (código inmutable, roles de sistema, permisos
 * bloqueados de ADMIN) viven en {@link Role}; aquí se comprueba lo que depende de otros datos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String code = Role.normalizeCode(request.code());
        if (roleRepository.existsByCode(code)) {
            throw new RoleAlreadyExistsException(code);
        }

        Role role = roleRepository.save(new Role(code, request.name(), request.description()));
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        Role role = findRole(id);
        role.update(request.name(), request.description());
        // flush para que la respuesta lleve el updatedAt que asigna la auditoría al guardar
        roleRepository.flush();
        return roleMapper.toResponse(role);
    }

    /**
     * Reemplaza el conjunto de permisos del rol.
     *
     * @throws PermissionNotFoundException        si algún permiso no existe (404); se informa el de menor id
     * @throws AdminRoleLockedPermissionException si se intenta quitar a ADMIN un permiso bloqueado (409)
     */
    @Transactional
    public RoleResponse replacePermissions(Long id, ReplaceRolePermissionsRequest request) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        Map<Long, Permission> found = permissionRepository.findAllById(request.permissionIds()).stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));
        request.permissionIds().stream()
                .filter(permissionId -> !found.containsKey(permissionId))
                .min(Long::compare)
                .ifPresent(missingId -> {
                    throw new PermissionNotFoundException(missingId);
                });

        role.replacePermissions(new HashSet<>(found.values()));
        roleRepository.flush();
        return roleMapper.toResponse(role);
    }

    public RoleResponse getById(Long id) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return roleMapper.toResponse(role);
    }

    /** Los permisos de la página se cargan por lotes ({@code @BatchSize} en {@link Role}). */
    public PageResponse<RoleResponse> list(Pageable pageable) {
        return PageResponse.from(roleRepository.findAll(pageable).map(roleMapper::toResponse));
    }

    /**
     * Borrado físico: un rol no tiene historial que conservar. Sus asignaciones de permisos se
     * borran en cascada (ON DELETE CASCADE en role_permissions).
     *
     * @throws SystemRoleException si es un rol de sistema (409)
     */
    @Transactional
    public void delete(Long id) {
        Role role = findRole(id);
        role.assertDeletable();
        roleRepository.delete(role);
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }

}
