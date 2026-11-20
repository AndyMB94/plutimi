-- Usuarios de la aplicación (módulo user). Cada usuario tiene exactamente un rol.
-- Los usuarios no se borran: se desactivan, porque pedidos, mascotas y citas los referencian.

CREATE TABLE users (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY,
    email         VARCHAR(255) NOT NULL,
    -- Hash BCrypt (60 caracteres); 100 deja margen si se cambia de algoritmo.
    -- Pasará a ser nullable cuando haya login con Google
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role_id       BIGINT       NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    -- El email se guarda en minúsculas, así que la unicidad simple ya no distingue mayúsculas
    CONSTRAINT uk_users_email UNIQUE (email),
    -- La entidad convierte el email a minúsculas; el CHECK lo exige también en SQL directo
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    -- Sin ON DELETE: la base de datos impide borrar un rol que todavía tiene usuarios
    -- (el service lo comprueba antes para devolver un 409 con un mensaje claro)
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- PostgreSQL no indexa automáticamente las FK; se usa en el filtro por rol, en "¿tiene el rol
-- usuarios?" y al contar los administradores activos
CREATE INDEX ix_users_role_id ON users (role_id);
