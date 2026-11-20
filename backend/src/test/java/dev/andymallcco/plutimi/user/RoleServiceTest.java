package dev.andymallcco.plutimi.user;

import static dev.andymallcco.plutimi.user.UserFixtures.permission;
import static dev.andymallcco.plutimi.user.UserFixtures.role;
import static dev.andymallcco.plutimi.user.UserFixtures.systemRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.user.dto.CreateRoleRequest;
import dev.andymallcco.plutimi.user.dto.PermissionResponse;
import dev.andymallcco.plutimi.user.dto.ReplaceRolePermissionsRequest;
import dev.andymallcco.plutimi.user.dto.RoleResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    private static final Long ID = 10L;
    private static final Permission CATALOG_MANAGE = permission(1, "catalog:manage");
    private static final Permission USER_READ = permission(3, "user:read");

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, permissionRepository, userRepository,
                new RoleMapper(new PermissionMapper()));
    }

    @Test
    void createSavesANonSystemRoleWithUppercaseCode() {
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleService.create(new CreateRoleRequest("stock_manager", "Encargado", null));

        assertThat(response.code()).isEqualTo("STOCK_MANAGER");
        assertThat(response.system()).isFalse();
        assertThat(response.permissions()).isEmpty();
    }

    @Test
    void createRejectsAnExistingCodeWithoutSaving() {
        when(roleRepository.existsByCode("STOCK_MANAGER")).thenReturn(true);

        assertThatThrownBy(() -> roleService.create(new CreateRoleRequest("Stock_Manager", "Encargado", null)))
                .isInstanceOf(RoleAlreadyExistsException.class)
                .hasMessage("Ya existe un rol con el código 'STOCK_MANAGER'");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void replacePermissionsReturnsThemSortedByCode() {
        Role role = role(ID, "STOCK_MANAGER");
        Set<Long> ids = Set.of(1L, 3L);
        when(roleRepository.findWithPermissionsById(ID)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(ids)).thenReturn(List.of(USER_READ, CATALOG_MANAGE));

        RoleResponse response = roleService.replacePermissions(ID, new ReplaceRolePermissionsRequest(ids));

        assertThat(response.permissions()).extracting(PermissionResponse::code)
                .containsExactly("catalog:manage", "user:read");
        verify(roleRepository).flush();
    }

    @Test
    void replacePermissionsReportsTheLowestMissingIdAndLeavesTheRoleUntouched() {
        Role role = role(ID, "STOCK_MANAGER");
        Set<Long> ids = Set.of(1L, 9L, 7L);
        when(roleRepository.findWithPermissionsById(ID)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(ids)).thenReturn(List.of(CATALOG_MANAGE));

        assertThatThrownBy(() -> roleService.replacePermissions(ID, new ReplaceRolePermissionsRequest(ids)))
                .isInstanceOf(PermissionNotFoundException.class)
                .hasMessage("No existe un permiso con id 7");
        assertThat(role.getPermissions()).isEmpty();
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        when(roleRepository.findWithPermissionsById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getById(ID))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessage("No existe un rol con id 10");
    }

    @Test
    void deleteRejectsASystemRole() {
        when(roleRepository.findById(ID)).thenReturn(Optional.of(systemRole(ID, "VET")));

        assertThatThrownBy(() -> roleService.delete(ID)).isInstanceOf(SystemRoleException.class);
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsARoleThatHasUsers() {
        when(roleRepository.findById(ID)).thenReturn(Optional.of(role(ID, "STOCK_MANAGER")));
        when(userRepository.existsByRoleId(ID)).thenReturn(true);

        assertThatThrownBy(() -> roleService.delete(ID))
                .isInstanceOf(RoleHasUsersException.class)
                .hasMessageContaining("'STOCK_MANAGER'");
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesACustomRoleWithoutUsers() {
        Role role = role(ID, "STOCK_MANAGER");
        when(roleRepository.findById(ID)).thenReturn(Optional.of(role));

        roleService.delete(ID);

        verify(roleRepository).delete(role);
    }

}
