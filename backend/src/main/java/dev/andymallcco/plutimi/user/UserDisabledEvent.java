package dev.andymallcco.plutimi.user;

/**
 * Se publica cuando se desactiva un usuario. En la 1.7, el módulo {@code auth} lo escucha para
 * revocar sus refresh tokens: pierde el acceso en cuanto caduca su access token.
 */
public record UserDisabledEvent(Long userId) {
}
