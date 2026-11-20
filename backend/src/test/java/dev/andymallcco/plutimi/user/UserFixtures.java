package dev.andymallcco.plutimi.user;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Objetos del módulo para los tests unitarios. Los ids y el flag {@code system} solo los asigna
 * la base de datos, así que aquí se fijan por reflexión en lugar de abrir setters en las entidades.
 */
final class UserFixtures {

    private UserFixtures() {
    }

    static Permission permission(long id, String code) {
        // El constructor protegido es accesible desde el mismo paquete
        Permission permission = new Permission();
        ReflectionTestUtils.setField(permission, "id", id);
        ReflectionTestUtils.setField(permission, "code", code);
        return permission;
    }

    static Role role(long id, String code) {
        Role role = new Role(code, code, null);
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }

    static Role systemRole(long id, String code) {
        Role role = role(id, code);
        ReflectionTestUtils.setField(role, "system", true);
        return role;
    }

    static User user(long id, String email, Role role) {
        User user = new User(email, "hash", "Ana", "Torres", null, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

}
