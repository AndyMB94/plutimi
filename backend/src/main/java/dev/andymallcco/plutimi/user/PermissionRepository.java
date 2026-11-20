package dev.andymallcco.plutimi.user;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private: los demás módulos no leen permisos directamente. Solo lectura en la práctica:
 * {@link Permission} es {@code @Immutable} y los permisos se crean con migraciones.
 */
interface PermissionRepository extends JpaRepository<Permission, Long> {
}
