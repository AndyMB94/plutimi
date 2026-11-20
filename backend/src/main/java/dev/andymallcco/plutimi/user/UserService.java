package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.ChangeUserRoleRequest;
import dev.andymallcco.plutimi.user.dto.ChangeUserStatusRequest;
import dev.andymallcco.plutimi.user.dto.UserFilter;
import dev.andymallcco.plutimi.user.dto.UserProfileResponse;
import dev.andymallcco.plutimi.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de usuarios: alta, consulta y administración (rol y estado). El perfil propio se
 * expone en la 1.7 con {@code /users/me}.
 * <p>
 * Regla que depende de otros usuarios: siempre debe quedar al menos un administrador activo.
 * Los efectos en otros módulos (revocar refresh tokens) se comunican con eventos, así
 * {@code user} no depende de {@code auth}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    /**
     * Da de alta un usuario con un rol de sistema. Lo usan {@code AdminInitializer} (ADMIN) y, en
     * la 1.7, el registro (CUSTOMER).
     *
     * @throws EmailAlreadyRegisteredException si el email ya está registrado (409)
     */
    @Transactional
    public UserResponse create(NewUser newUser, String roleCode) {
        String email = User.normalizeEmail(newUser.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        // Los roles de sistema vienen de V7: si falta, la base de datos está mal, no la petición
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleCode));

        User user = new User(email, passwordEncoder.encode(newUser.rawPassword()),
                newUser.firstName(), newUser.lastName(), newUser.phone(), role);
        return userMapper.toResponse(userRepository.save(user));
    }

    /** ¿Hay algún usuario con el rol ADMIN, activo o no? */
    public boolean adminExists() {
        return userRepository.existsByRoleCode(Role.ADMIN_CODE);
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUserWithRole(id));
    }

    /** Perfil con los permisos del rol. Lo usa {@code GET /users/me} en la 1.7. */
    public UserProfileResponse getProfile(Long userId) {
        return userMapper.toProfileResponse(findUserWithRole(userId));
    }

    public PageResponse<UserResponse> list(UserFilter filter, Pageable pageable) {
        Page<User> users = userRepository.findAll(UserSpecifications.from(filter), pageable);
        return PageResponse.from(users.map(userMapper::toResponse));
    }

    /**
     * Cambia el rol. Si el rol no cambia, no hace nada (ni publica el evento).
     *
     * @throws RoleNotFoundException     si el rol nuevo no existe (404)
     * @throws LastActiveAdminException  si es el último administrador activo y el rol nuevo no es ADMIN (409)
     */
    @Transactional
    public UserResponse changeRole(Long id, ChangeUserRoleRequest request) {
        User user = findUserWithRole(id);
        Role newRole = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException(request.roleId()));
        if (user.getRole().getId().equals(newRole.getId())) {
            return userMapper.toResponse(user);
        }
        if (user.isActiveAdmin() && !newRole.isAdmin()) {
            assertNotLastActiveAdmin();
        }

        user.changeRole(newRole);
        eventPublisher.publishEvent(new UserRoleChangedEvent(user.getId()));
        userRepository.flush();
        return userMapper.toResponse(user);
    }

    /**
     * Activa o desactiva la cuenta. Es idempotente: si ya está en ese estado, no hace nada.
     *
     * @throws LastActiveAdminException si se intenta desactivar al último administrador activo (409)
     */
    @Transactional
    public UserResponse changeStatus(Long id, ChangeUserStatusRequest request) {
        User user = findUserWithRole(id);
        if (request.enabled() == user.isEnabled()) {
            return userMapper.toResponse(user);
        }

        if (request.enabled()) {
            user.enable();
        } else {
            if (user.isActiveAdmin()) {
                assertNotLastActiveAdmin();
            }
            user.disable();
            eventPublisher.publishEvent(new UserDisabledEvent(user.getId()));
        }
        userRepository.flush();
        return userMapper.toResponse(user);
    }

    private User findUserWithRole(Long id) {
        return userRepository.findWithRoleById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /** Se llama cuando el usuario es un admin activo: si es el único, no puede dejar de serlo. */
    private void assertNotLastActiveAdmin() {
        if (userRepository.countByRoleCodeAndEnabledTrue(Role.ADMIN_CODE) <= 1) {
            throw new LastActiveAdminException();
        }
    }

}
