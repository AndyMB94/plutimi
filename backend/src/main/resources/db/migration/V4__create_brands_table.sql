-- Marcas del catálogo (módulo catalog).
-- Las marcas no se borran: se desactivan (active = false) porque los productos las referencian.

CREATE TABLE brands (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
);

-- Nombre único sin distinguir mayúsculas: "Royal Canin" y "royal canin" son la misma marca.
CREATE UNIQUE INDEX uk_brands_name ON brands (lower(name));
