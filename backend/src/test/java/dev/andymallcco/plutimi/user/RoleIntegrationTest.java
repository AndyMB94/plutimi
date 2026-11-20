package dev.andymallcco.plutimi.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.user.dto.ChangeUserRoleRequest;
import dev.andymallcco.plutimi.user.dto.CreateRoleRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Roles y permisos de punta a punta contra PostgreSQL, partiendo de los datos de referencia de V7.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RoleIntegrationTest {

    private static final String ROLES = "/api/v1/roles";
    private static final String PERMISSIONS = "/api/v1/permissions";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        // Primero los usuarios: la FK fk_users_role impide borrar roles con usuarios.
        // Los roles de sistema son datos de referencia (V7) y se conservan.
        userRepository.deleteAll();
        roleRepository.deleteAll(roleRepository.findAll().stream().filter(role -> !role.isSystem()).toList());
    }

    @Test
    void referenceDataHasTheSystemRolesAndTheAdminPermissions() {
        MvcTestResult result = mvc.get().uri(ROLES).exchange();

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].code").asArray().containsExactly("ADMIN", "CUSTOMER", "VET");
        assertThat(result)
                .bodyJson().extractingPath("$.content[0].permissions[*].code").asArray()
                .containsExactly("catalog:manage", "role:manage", "species:manage", "user:manage", "user:read");
        assertThat(result)
                .bodyJson().extractingPath("$.content[*].system").asArray().containsOnly(true);
    }

    @Test
    void permissionsCatalogIsSortedByCode() {
        assertThat(mvc.get().uri(PERMISSIONS))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[*].code").asArray()
                .containsExactly("catalog:manage", "role:manage", "species:manage", "user:manage", "user:read");
    }

    @Test
    void createReturns201WithLocationAndUppercaseCode() throws Exception {
        MvcTestResult result = mvc.post().uri(ROLES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "stock_manager", "name": "Encargado de almacén" }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "code": "STOCK_MANAGER", "name": "Encargado de almacén", "system": false, "permissions": [] }
                        """);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(ROLES + "/" + idFrom(result));
    }

    @Test
    void duplicateCodeIgnoringCaseReturns409() {
        createRole("STOCK_MANAGER");

        assertThat(mvc.post().uri(ROLES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "Stock_Manager", "name": "Otro" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe un rol con el código 'STOCK_MANAGER'" }
                        """);
    }

    @Test
    void replacePermissionsAssignsThem() {
        long id = createRole("STOCK_MANAGER");

        assertThat(mvc.put().uri(ROLES + "/{id}/permissions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "permissionIds": [%d] }
                        """.formatted(permissionId("catalog:manage"))))
                .hasStatusOk()
                .bodyJson().extractingPath("$.permissions[*].code").asArray().containsExactly("catalog:manage");
    }

    @Test
    void unknownPermissionReturns404() {
        long id = createRole("STOCK_MANAGER");

        assertThat(mvc.put().uri(ROLES + "/{id}/permissions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "permissionIds": [999999] }
                        """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe un permiso con id 999999" }
                        """);
    }

    @Test
    void adminCannotLoseLockedPermissions() {
        long adminId = roleId("ADMIN");

        assertThat(mvc.put().uri(ROLES + "/{id}/permissions", adminId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "permissionIds": [%d] }
                        """.formatted(permissionId("catalog:manage"))))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "El rol ADMIN debe conservar los permisos role:manage, user:manage" }
                        """);

        assertThat(mvc.get().uri(ROLES + "/{id}", adminId))
                .bodyJson().extractingPath("$.permissions").asArray().hasSize(5);
    }

    @Test
    void deleteSystemRoleReturns409() {
        assertThat(mvc.delete().uri(ROLES + "/{id}", roleId("VET")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "El rol 'VET' es un rol del sistema y no se puede borrar" }
                        """);
    }

    @Test
    void deleteRoleWithUsersReturns409() {
        long roleId = createRole("STOCK_MANAGER");
        long userId = userService.create(
                new NewUser("ana@example.com", "secret-password", "Ana", "Torres", null), "CUSTOMER").id();
        userService.changeRole(userId, new ChangeUserRoleRequest(roleId));

        assertThat(mvc.delete().uri(ROLES + "/{id}", roleId))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "El rol 'STOCK_MANAGER' tiene usuarios asignados: cámbiales el rol antes de borrarlo" }
                        """);
    }

    @Test
    void deleteCustomRoleReturns204AndRemovesIt() {
        long id = createRole("STOCK_MANAGER");

        assertThat(mvc.delete().uri(ROLES + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(ROLES + "/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private long createRole(String code) {
        return roleService.create(new CreateRoleRequest(code, "Rol " + code, null)).id();
    }

    private long roleId(String code) {
        return roleRepository.findByCode(code).orElseThrow().getId();
    }

    private long permissionId(String code) {
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getCode().equals(code))
                .findFirst().orElseThrow()
                .getId();
    }

    private static long idFrom(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

}
