package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.text.LikePatterns;
import dev.andymallcco.plutimi.user.dto.UserFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criterios del listado de usuarios (patrón Specification, como {@code ProductSpecifications}).
 */
final class UserSpecifications {

    private UserSpecifications() {
    }

    static Specification<User> from(UserFilter filter) {
        Specification<User> spec = Specification.unrestricted();
        if (filter.roleId() != null) {
            spec = spec.and(hasRole(filter.roleId()));
        }
        if (filter.searchText() != null) {
            spec = spec.and(emailOrNameContains(filter.searchText()));
        }
        return spec;
    }

    static Specification<User> hasRole(Long roleId) {
        // role.id se lee de la columna role_id: no hace falta JOIN con roles
        return (root, query, cb) -> cb.equal(root.get("role").get("id"), roleId);
    }

    /** Equivale a {@code email ILIKE '%texto%' OR first_name ILIKE ... OR last_name ILIKE ...}. */
    static Specification<User> emailOrNameContains(String text) {
        String pattern = LikePatterns.containing(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(root.get("firstName")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(root.get("lastName")), pattern, LikePatterns.ESCAPE));
    }

}
