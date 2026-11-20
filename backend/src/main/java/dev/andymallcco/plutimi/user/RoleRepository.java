package dev.andymallcco.plutimi.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private: los demás módulos acceden a los roles a través de los services de {@code user}.
 */
interface RoleRepository extends JpaRepository<Role, Long> {

    // El código se guarda normalizado en mayúsculas: basta la comparación exacta
    boolean existsByCode(String code);

    /**
     * Un rol con sus permisos en una sola consulta (LEFT JOIN). Aquí sí se puede usar un
     * {@code @EntityGraph} sobre una colección porque no hay paginación (ver
     * {@code Role.permissions} para el listado).
     */
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(Long id);

}
