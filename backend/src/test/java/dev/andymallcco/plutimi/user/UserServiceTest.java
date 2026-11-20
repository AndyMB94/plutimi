package dev.andymallcco.plutimi.user;

import static dev.andymallcco.plutimi.user.UserFixtures.systemRole;
import static dev.andymallcco.plutimi.user.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.user.dto.ChangeUserRoleRequest;
import dev.andymallcco.plutimi.user.dto.ChangeUserStatusRequest;
import dev.andymallcco.plutimi.user.dto.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ID = 5L;
    private static final Role CUSTOMER = systemRole(1, "CUSTOMER");
    private static final Role ADMIN = systemRole(3, "ADMIN");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, new UserMapper(), eventPublisher, passwordEncoder);
    }

    // --- Alta ---

    @Test
    void createStoresTheHashedPasswordAndTheRole() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(CUSTOMER));
        when(passwordEncoder.encode("secret-password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(new NewUser(" Ana@Example.com ", "secret-password", "Ana", "Torres", null), "CUSTOMER");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isSameAs(CUSTOMER);
    }

    @Test
    void createRejectsARegisteredEmailWithoutHashing() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new NewUser("ANA@example.com", "secret-password", "Ana", "Torres", null), "CUSTOMER"))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Ya existe una cuenta con el email 'ana@example.com'");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    // --- Cambio de rol ---

    @Test
    void changeRoleToTheSameRoleDoesNothing() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "ana@example.com", CUSTOMER)));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(CUSTOMER));

        userService.changeRole(ID, new ChangeUserRoleRequest(1L));

        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).flush();
    }

    @Test
    void changeRoleRejectsUnknownRole() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "ana@example.com", CUSTOMER)));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(ID, new ChangeUserRoleRequest(99L)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void changeRoleRejectsDemotingTheLastActiveAdmin() {
        User admin = user(ID, "admin@example.com", ADMIN);
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(admin));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(CUSTOMER));
        when(userRepository.countByRoleCodeAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeRole(ID, new ChangeUserRoleRequest(1L)))
                .isInstanceOf(LastActiveAdminException.class);
        assertThat(admin.getRole()).isSameAs(ADMIN);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changeRoleDemotesAnAdminWhenAnotherActiveAdminExists() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "admin@example.com", ADMIN)));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(CUSTOMER));
        when(userRepository.countByRoleCodeAndEnabledTrue("ADMIN")).thenReturn(2L);

        UserResponse response = userService.changeRole(ID, new ChangeUserRoleRequest(1L));

        assertThat(response.role().code()).isEqualTo("CUSTOMER");
        verify(eventPublisher).publishEvent(new UserRoleChangedEvent(ID));
    }

    @Test
    void promotingToAdminDoesNotCountAdmins() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "ana@example.com", CUSTOMER)));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(ADMIN));

        UserResponse response = userService.changeRole(ID, new ChangeUserRoleRequest(3L));

        assertThat(response.role().code()).isEqualTo("ADMIN");
        verify(userRepository, never()).countByRoleCodeAndEnabledTrue(anyString());
        verify(eventPublisher).publishEvent(new UserRoleChangedEvent(ID));
    }

    // --- Cambio de estado ---

    @Test
    void changeStatusRejectsDisablingTheLastActiveAdmin() {
        User admin = user(ID, "admin@example.com", ADMIN);
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleCodeAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeStatus(ID, new ChangeUserStatusRequest(false)))
                .isInstanceOf(LastActiveAdminException.class);
        assertThat(admin.isEnabled()).isTrue();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void disablingAUserPublishesAnEvent() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "ana@example.com", CUSTOMER)));

        UserResponse response = userService.changeStatus(ID, new ChangeUserStatusRequest(false));

        assertThat(response.enabled()).isFalse();
        verify(eventPublisher).publishEvent(new UserDisabledEvent(ID));
    }

    @Test
    void enablingAUserDoesNotPublishEvents() {
        User user = user(ID, "ana@example.com", CUSTOMER);
        user.disable();
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user));

        UserResponse response = userService.changeStatus(ID, new ChangeUserStatusRequest(true));

        assertThat(response.enabled()).isTrue();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changeStatusIsIdempotent() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.of(user(ID, "ana@example.com", CUSTOMER)));

        userService.changeStatus(ID, new ChangeUserStatusRequest(true));

        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).flush();
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        when(userRepository.findWithRoleById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("No existe un usuario con id 5");
    }

}
