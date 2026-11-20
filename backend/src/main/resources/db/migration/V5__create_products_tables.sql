-- Productos del catálogo y especies a las que van dirigidos (módulo catalog).
-- Los productos no se borran: se desactivan, porque los pedidos los referencian.

CREATE TABLE products (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY,
    category_id    BIGINT        NOT NULL,
    brand_id       BIGINT,
    sku            VARCHAR(50)   NOT NULL,
    name           VARCHAR(150)  NOT NULL,
    slug           VARCHAR(210)  NOT NULL,
    description    TEXT,
    price_amount   NUMERIC(12,2) NOT NULL,
    price_currency VARCHAR(3)    NOT NULL,
    stock          INTEGER       NOT NULL,
    image_url      VARCHAR(500),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    -- Marca opcional: productos genéricos o de fabricación propia
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    -- El SKU se guarda en mayúsculas, así que la unicidad simple ya no distingue mayúsculas
    CONSTRAINT uk_products_sku UNIQUE (sku),
    -- La entidad convierte el SKU a mayúsculas; el CHECK lo exige también en SQL directo
    CONSTRAINT ck_products_sku_uppercase CHECK (sku = upper(sku)),
    -- El slug se deriva de nombre + SKU: es único aunque dos productos se llamen igual
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT ck_products_price_amount_positive CHECK (price_amount > 0),
    -- VARCHAR(3) y no CHAR(3): Hibernate mapea Currency a VARCHAR y ddl-auto=validate
    -- rechazaría un CHAR. El CHECK garantiza igualmente un código ISO 4217 (PEN, USD...)
    CONSTRAINT ck_products_price_currency_format CHECK (price_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock >= 0)
);

-- PostgreSQL no indexa automáticamente las FK; estas columnas se usan en los filtros del catálogo
-- y en la comprobación "¿tiene la categoría productos activos?"
CREATE INDEX ix_products_category_id ON products (category_id);
CREATE INDEX ix_products_brand_id ON products (brand_id);

CREATE TABLE product_species (
    product_id BIGINT NOT NULL,
    species_id BIGINT NOT NULL,
    -- PK compuesta: impide repetir la misma especie en un producto
    CONSTRAINT pk_product_species PRIMARY KEY (product_id, species_id),
    CONSTRAINT fk_product_species_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    -- Especie de otro módulo: la FK protege la integridad aunque en Java solo se guarde el ID
    CONSTRAINT fk_product_species_species FOREIGN KEY (species_id) REFERENCES species (id)
);

-- La PK empieza por product_id; el filtro "productos para gatos" busca por species_id
CREATE INDEX ix_product_species_species_id ON product_species (species_id);
