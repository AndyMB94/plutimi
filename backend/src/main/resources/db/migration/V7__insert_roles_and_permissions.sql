-- Datos de referencia: roles base, permisos de la fase 1 y su asignación (todos los entornos).
-- Las asignaciones se hacen por code, nunca por id: el id puede variar entre entornos.
-- Las fases siguientes añaden sus permisos en migraciones propias (insert_order_permissions...).

INSERT INTO roles (code, name, description, system)
VALUES ('CUSTOMER', 'Cliente', 'Compra en la tienda y gestiona sus mascotas y citas', TRUE),
       ('VET', 'Veterinario', 'Atiende citas y registra el historial clínico', TRUE),
       ('ADMIN', 'Administrador', 'Gestiona el catálogo, los usuarios y los roles', TRUE);

INSERT INTO permissions (code, description)
VALUES ('catalog:manage', 'Gestionar categorías, marcas y productos, y ajustar el stock'),
       ('species:manage', 'Gestionar especies y razas'),
       ('user:read', 'Listar y ver usuarios'),
       ('user:manage', 'Cambiar el rol o el estado de un usuario'),
       ('role:manage', 'Crear roles y asignarles permisos');

-- En la fase 1 solo ADMIN tiene permisos: CUSTOMER y VET reciben los suyos en las fases 2 y 3
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('catalog:manage',
                                          'species:manage',
                                          'user:read',
                                          'user:manage',
                                          'role:manage')
WHERE r.code = 'ADMIN';
