package dev.andymallcco.plutimi.user;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Package-private: los demás módulos acceden a los usuarios a través de UserService.
 */
interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /** Listado con el rol en la misma consulta (JOIN a-uno, compatible con la paginación). */
    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(@Nullable Specification<User> spec, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(Long id);

    boolean existsByRoleId(Long roleId);

    boolean existsByRoleCode(String roleCode);

    // El email se guarda normalizado en minúsculas: basta la comparación exacta
    boolean existsByEmail(String email);

    /** Spring Data genera el JOIN con roles a partir del nombre: role.code = ? AND enabled = true. */
    long countByRoleCodeAndEnabledTrue(String roleCode);

}
