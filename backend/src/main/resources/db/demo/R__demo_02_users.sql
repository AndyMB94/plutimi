-- Datos de demostración de usuarios: uno de cada rol base. Solo perfil dev.
--
-- Migración repetible (R__), idempotente como R__demo_01_catalog.sql:
--   * INSERT ... ON CONFLICT (email) DO NOTHING: no duplica ni pisa lo ya existente.
--   * El rol se busca por code, nunca por id fijo.
--
-- Los tres comparten la contraseña de desarrollo documentada en el README, guardada aquí como
-- hash BCrypt (coste 10, el mismo que PasswordEncoderConfig). No es un secreto: estos usuarios
-- solo existen en dev. Para regenerar el hash, ver el comando con jshell en el README.

INSERT INTO users (email, password_hash, first_name, last_name, phone, role_id)
SELECT u.email, demo.password_hash, u.first_name, u.last_name, u.phone, r.id
FROM (VALUES ('admin@plutimi.dev', 'Lucía', 'Ramos', NULL, 'ADMIN'),
             ('vet@plutimi.dev', 'Diego', 'Quispe', '987654321', 'VET'),
             ('cliente@plutimi.dev', 'Carla', 'Mendoza', '912345678', 'CUSTOMER'))
         AS u (email, first_name, last_name, phone, role_code)
         JOIN roles r ON r.code = u.role_code
         -- Un solo lugar para el hash compartido
         CROSS JOIN (VALUES ('$2a$10$tRY2KwT4MRUwzGFbWU3z0elSG6K3J48yoDHKQHUfC5IMFHt2YeeOq')) AS demo (password_hash)
ON CONFLICT (email) DO NOTHING;
