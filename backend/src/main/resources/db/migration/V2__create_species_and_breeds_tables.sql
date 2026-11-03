-- Especies y razas (módulo species). Datos de referencia compartidos por el catálogo y la veterinaria.
-- No se borran: se desactivan (active = false) porque productos y mascotas los referencian.

CREATE TABLE species (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY,
    code       VARCHAR(30) NOT NULL,
    name       VARCHAR(60) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_species PRIMARY KEY (id),
    CONSTRAINT uk_species_code UNIQUE (code),
    -- El código es la referencia estable que usan el código y los seeders: DOG, CAT, GUINEA_PIG...
    CONSTRAINT ck_species_code_format CHECK (code ~ '^[A-Z][A-Z_]*$')
);

-- Nombre único sin distinguir mayúsculas: "Perro" y "perro" son la misma especie.
CREATE UNIQUE INDEX uk_species_name ON species (lower(name));

CREATE TABLE breeds (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY,
    species_id BIGINT      NOT NULL,
    name       VARCHAR(80) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_breeds PRIMARY KEY (id),
    CONSTRAINT fk_breeds_species FOREIGN KEY (species_id) REFERENCES species (id),
    -- Destino de la FK compuesta (breed_id, species_id) de pets: garantiza que la raza
    -- de una mascota pertenece a su especie.
    CONSTRAINT uk_breeds_id_species_id UNIQUE (id, species_id)
);

-- Nombre único dentro de cada especie, sin distinguir mayúsculas. Como empieza por species_id,
-- también sirve al listado de razas de una especie (no hace falta un índice aparte).
CREATE UNIQUE INDEX uk_breeds_species_id_name ON breeds (species_id, lower(name));
