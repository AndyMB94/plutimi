package dev.andymallcco.plutimi.user;

import static dev.andymallcco.plutimi.user.UserFixtures.permission;
import static dev.andymallcco.plutimi.user.UserFixtures.role;
import static dev.andymallcco.plutimi.user.UserFixtures.systemRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleTest {

    private static final Permission CATALOG_MANAGE = permission(1, "catalog:manage");
    private static final Permission USER_READ = permission(3, "user:read");
    private static final Permission USER_MANAGE = permission(4, "user:manage");
    private static final Permission ROLE_MANAGE = permission(5, "role:manage");

    @Test
    void constructorNormalizesCodeAndTextsAndCreatesANonSystemRole() {
        Role role = new Role("  stock_manager ", "  Encargado de almacén ", "   ");

        assertThat(role.getCode()).isEqualTo("STOCK_MANAGER");
        assertThat(role.getName()).isEqualTo("Encargado de almacén");
        assertThat(role.getDescription()).isNull();
        assertThat(role.isSystem()).isFalse();
        assertThat(role.getPermissions()).isEmpty();
    }

    @Test
    void replacePermissionsReplacesTheWholeSet() {
        Role role = role(10, "STOCK_MANAGER");
        role.replacePermissions(Set.of(CATALOG_MANAGE, USER_READ));

        role.replacePermissions(Set.of(USER_READ, USER_MANAGE));

        assertThat(role.getPermissions()).containsExactlyInAnyOrder(USER_READ, USER_MANAGE);
    }

    @Test
    void customRoleCanBeLeftWithoutPermissions() {
        Role role = role(10, "STOCK_MANAGER");
        role.replacePermissions(Set.of(CATALOG_MANAGE));

        role.replacePermissions(Set.of());

        assertThat(role.getPermissions()).isEmpty();
    }

    @Test
    void adminCanChangePermissionsWhileKeepingTheLockedOnes() {
        Role admin = systemRole(3, "ADMIN");

        admin.replacePermissions(Set.of(CATALOG_MANAGE, USER_MANAGE, ROLE_MANAGE));

        assertThat(admin.getPermissions()).containsExactlyInAnyOrder(CATALOG_MANAGE, USER_MANAGE, ROLE_MANAGE);
    }

    @Test
    void adminCannotLoseALockedPermissionAndKeepsItsCurrentOnes() {
        Role admin = systemRole(3, "ADMIN");
        admin.replacePermissions(Set.of(CATALOG_MANAGE, USER_MANAGE, ROLE_MANAGE));

        assertThatThrownBy(() -> admin.replacePermissions(Set.of(CATALOG_MANAGE, USER_MANAGE)))
                .isInstanceOf(AdminRoleLockedPermissionException.class)
                .hasMessage("El rol ADMIN debe conservar los permisos role:manage");
        assertThat(admin.getPermissions()).containsExactlyInAnyOrder(CATALOG_MANAGE, USER_MANAGE, ROLE_MANAGE);
    }

    @Test
    void adminErrorListsEveryMissingLockedPermissionSorted() {
        Role admin = systemRole(3, "ADMIN");

        assertThatThrownBy(() -> admin.replacePermissions(Set.of(CATALOG_MANAGE)))
                .hasMessage("El rol ADMIN debe conservar los permisos role:manage, user:manage");
    }

    @Test
    void systemRoleIsNotDeletable() {
        Role customer = systemRole(1, "CUSTOMER");

        assertThatThrownBy(customer::assertDeletable)
                .isInstanceOf(SystemRoleException.class)
                .hasMessage("El rol 'CUSTOMER' es un rol del sistema y no se puede borrar");
    }

    @Test
    void customRoleIsDeletable() {
        assertThatCode(() -> role(10, "STOCK_MANAGER").assertDeletable()).doesNotThrowAnyException();
    }

    @Test
    void permissionsViewIsReadOnly() {
        Role role = role(10, "STOCK_MANAGER");

        assertThatThrownBy(() -> role.getPermissions().add(CATALOG_MANAGE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
