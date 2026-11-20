package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.PermissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo de permisos, de solo lectura: se crean con migraciones. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PageResponse<PermissionResponse> list(Pageable pageable) {
        return PageResponse.from(permissionRepository.findAll(pageable).map(permissionMapper::toResponse));
    }

}
