-- Control de acceso basado en roles (módulo user).
-- Cada usuario tiene un rol; cada rol agrupa N permisos (role_permissions, N:M).

CREATE TABLE roles (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(255),
    -- Roles base (CUSTOMER, VET, ADMIN): no se pueden borrar
    system      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    -- El código es la clave estable que usa el código Java (CUSTOMER, VET): mayúsculas y guion bajo
    CONSTRAINT ck_roles_code_format CHECK (code ~ '^[A-Z][A-Z0-9_]*$')
);

-- Solo se crean con migraciones: un permiso sin código Java que lo compruebe no sirve de nada.
-- Por eso no tiene updated_at.
CREATE TABLE permissions (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY,
    code        VARCHAR(60)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code),
    -- Formato <recurso>:<acción> en minúsculas y kebab-case (catalog:manage, order:read-all)
    CONSTRAINT ck_permissions_code_format CHECK (code ~ '^[a-z]+(-[a-z]+)*:[a-z]+(-[a-z]+)*$')
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    -- PK compuesta: impide asignar dos veces el mismo permiso a un rol
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    -- Borrar un rol borra sus asignaciones (el service ya impide borrarlo si tiene usuarios)
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- La PK empieza por role_id; este índice cubre la búsqueda inversa ("¿qué roles tienen este
-- permiso?") y el ON DELETE CASCADE desde permissions
CREATE INDEX ix_role_permissions_permission_id ON role_permissions (permission_id);
