package dev.andymallcco.plutimi.user;

import static dev.andymallcco.plutimi.user.UserFixtures.systemRole;
import static dev.andymallcco.plutimi.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final Role CUSTOMER = systemRole(1, "CUSTOMER");
    private static final Role ADMIN = systemRole(3, "ADMIN");

    @Test
    void constructorNormalizesEmailNamesAndOptionalPhone() {
        User user = new User("  Ana.Torres@Example.COM ", "hash", " Ana ", " Torres ", "   ", CUSTOMER);

        assertThat(user.getEmail()).isEqualTo("ana.torres@example.com");
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("Torres");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getRole()).isSameAs(CUSTOMER);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void activeAdminNeedsTheAdminRoleAndAnEnabledAccount() {
        User admin = user(1, "admin@example.com", ADMIN);
        User customer = user(2, "ana@example.com", CUSTOMER);

        assertThat(admin.isActiveAdmin()).isTrue();
        assertThat(customer.isActiveAdmin()).isFalse();

        admin.disable();
        assertThat(admin.isActiveAdmin()).isFalse();
    }

    @Test
    void changeRoleReplacesTheRole() {
        User user = user(2, "ana@example.com", CUSTOMER);

        user.changeRole(ADMIN);

        assertThat(user.getRole()).isSameAs(ADMIN);
    }

    @Test
    void disableAndEnableToggleTheAccount() {
        User user = user(2, "ana@example.com", CUSTOMER);

        user.disable();
        assertThat(user.isEnabled()).isFalse();

        user.enable();
        assertThat(user.isEnabled()).isTrue();
    }

}
