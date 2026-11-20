package dev.andymallcco.plutimi.user;

/**
 * Se publica cuando cambia el rol de un usuario. En la 1.7, el módulo {@code auth} lo escucha para
 * revocar sus refresh tokens: así el siguiente login lee los permisos nuevos.
 */
public record UserRoleChangedEvent(Long userId) {
}
