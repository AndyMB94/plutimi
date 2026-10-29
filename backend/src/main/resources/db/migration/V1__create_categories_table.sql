-- Categorías del catálogo (módulo catalog).
-- Las categorías no se borran: se desactivan (active = false) porque los productos las referencian.

CREATE TABLE categories (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

-- Nombre único sin distinguir mayúsculas: "Juguetes" y "juguetes" son la misma categoría.
CREATE UNIQUE INDEX uk_categories_name ON categories (lower(name));
