-- Datos de demostración del catálogo: categorías, marcas y productos. Solo perfil dev.
--
-- Migración repetible (R__): Flyway la vuelve a ejecutar cada vez que cambia este archivo,
-- así que es idempotente:
--   * INSERT ... ON CONFLICT (clave natural) DO NOTHING: no duplica ni pisa lo ya existente
--     (tampoco los cambios hechos a mano en dev).
--   * Las FK se buscan por clave natural (slug, sku, code), nunca por id fijo.
--
-- Los slugs siguen la misma regla que shared/text/Slugs (minúsculas, sin tildes, guiones);
-- el de producto es nombre + SKU. Si se edita un producto por la API, Java lo regenera.
--
-- El catálogo incluye a propósito productos agotados (stock 0), sin marca, para varias
-- especies y uno desactivado, para probar todos los filtros desde Swagger.

-- --- Categorías ---

INSERT INTO categories (name, slug, description)
VALUES ('Alimento para perros', 'alimento-para-perros', 'Croquetas y alimento húmedo para perros de todas las edades'),
       ('Alimento para gatos', 'alimento-para-gatos', 'Alimento seco y húmedo para gatitos, adultos y senior'),
       ('Alimento para otras especies', 'alimento-para-otras-especies', 'Aves, conejos, roedores y peces'),
       ('Snacks y premios', 'snacks-y-premios', 'Premios para el adiestramiento y el cuidado dental'),
       ('Juguetes', 'juguetes', 'Juguetes resistentes y de estimulación mental'),
       ('Accesorios', 'accesorios', 'Comederos, correas, arneses y camas'),
       ('Higiene y cuidado', 'higiene-y-cuidado', 'Arena, champús y productos de limpieza')
ON CONFLICT (slug) DO NOTHING;

-- --- Marcas ---

INSERT INTO brands (name, slug)
VALUES ('Royal Canin', 'royal-canin'),
       ('Pro Plan', 'pro-plan'),
       ('Hill''s', 'hill-s'),
       ('Whiskas', 'whiskas'),
       ('Pedigree', 'pedigree'),
       ('Kong', 'kong'),
       ('Vitakraft', 'vitakraft')
ON CONFLICT (slug) DO NOTHING;

-- --- Productos ---
-- brand NULL = producto genérico sin marca.

INSERT INTO products (category_id, brand_id, sku, name, slug, description,
                      price_amount, price_currency, stock, active)
SELECT c.id, b.id, p.sku, p.name, p.slug, p.description, p.price, 'PEN', p.stock, p.active
FROM (VALUES
        ('alimento-para-perros', 'royal-canin', 'RC-MED-AD-15', 'Royal Canin Medium Adult 15 kg',
         'royal-canin-medium-adult-15-kg-rc-med-ad-15', 'Para perros adultos de raza mediana (11 a 25 kg)', 289.90, 12, TRUE),
        ('alimento-para-perros', 'royal-canin', 'RC-MINI-PUP-3', 'Royal Canin Mini Puppy 3 kg',
         'royal-canin-mini-puppy-3-kg-rc-mini-pup-3', 'Para cachorros de raza pequeña hasta los 10 meses', 119.90, 20, TRUE),
        ('alimento-para-perros', 'pro-plan', 'PP-AD-LG-15', 'Pro Plan Adulto Razas Grandes 15 kg',
         'pro-plan-adulto-razas-grandes-15-kg-pp-ad-lg-15', 'Con OptiHealth para perros adultos de razas grandes', 259.00, 0, TRUE),
        ('alimento-para-perros', 'pedigree', 'PED-AD-21', 'Pedigree Adulto Carne 21 kg',
         'pedigree-adulto-carne-21-kg-ped-ad-21', 'Alimento completo sabor carne para perros adultos', 169.90, 30, TRUE),
        ('alimento-para-perros', 'royal-canin', 'RC-OLD-1', 'Royal Canin Fórmula Descontinuada 2 kg',
         'royal-canin-formula-descontinuada-2-kg-rc-old-1', 'Producto retirado del catálogo', 99.90, 0, FALSE),
        ('alimento-para-gatos', 'royal-canin', 'RC-IND-4', 'Royal Canin Indoor 4 kg',
         'royal-canin-indoor-4-kg-rc-ind-4', 'Para gatos adultos que viven en interior', 159.90, 15, TRUE),
        ('alimento-para-gatos', 'whiskas', 'WHI-AD-POL-1', 'Whiskas Adulto Pollo 1.5 kg',
         'whiskas-adulto-pollo-1-5-kg-whi-ad-pol-1', 'Croquetas sabor pollo para gatos adultos', 32.50, 40, TRUE),
        ('alimento-para-gatos', 'hill-s', 'HIL-KIT-1', 'Hill''s Science Diet Kitten 1.6 kg',
         'hill-s-science-diet-kitten-1-6-kg-hil-kit-1', 'Para gatitos de hasta 1 año', 89.90, 8, TRUE),
        ('alimento-para-otras-especies', 'vitakraft', 'VK-MENU-CON', 'Vitakraft Menú Conejos 1 kg',
         'vitakraft-menu-conejos-1-kg-vk-menu-con', 'Mezcla de cereales y verduras para conejos enanos', 22.90, 12, TRUE),
        ('snacks-y-premios', 'pedigree', 'PED-DENTA-7', 'Pedigree Dentastix 7 unidades',
         'pedigree-dentastix-7-unidades-ped-denta-7', 'Barritas para la higiene dental diaria', 18.90, 50, TRUE),
        ('snacks-y-premios', 'vitakraft', 'VK-STICK-AVE', 'Vitakraft Barritas de Miel para Aves',
         'vitakraft-barritas-de-miel-para-aves-vk-stick-ave', 'Barritas de semillas con miel para periquitos', 14.50, 25, TRUE),
        ('juguetes', 'kong', 'KONG-CL-M', 'Kong Classic Mediano',
         'kong-classic-mediano-kong-cl-m', 'Juguete de caucho natural rellenable con premios', 59.90, 10, TRUE),
        ('juguetes', NULL, 'TOY-RAT-3', 'Ratones de Peluche x3',
         'ratones-de-peluche-x3-toy-rat-3', 'Con hierba gatera', 12.00, 35, TRUE),
        ('juguetes', NULL, 'TOY-PEL-GOMA', 'Pelota de Goma con Sonido',
         'pelota-de-goma-con-sonido-toy-pel-goma', 'Pelota de 6 cm para perros y gatos', 9.90, 0, TRUE),
        ('accesorios', NULL, 'ACC-COM-INOX', 'Comedero de Acero Inoxidable',
         'comedero-de-acero-inoxidable-acc-com-inox', 'Antideslizante, 750 ml', 24.90, 18, TRUE),
        ('accesorios', NULL, 'ACC-ARN-M', 'Arnés Acolchado Talla M',
         'arnes-acolchado-talla-m-acc-arn-m', 'Arnés en H con cierre de seguridad', 45.00, 6, TRUE),
        ('higiene-y-cuidado', NULL, 'HIG-ARENA-10', 'Arena Aglomerante 10 kg',
         'arena-aglomerante-10-kg-hig-arena-10', 'Arena de bentonita sin perfume', 39.90, 22, TRUE),
        ('higiene-y-cuidado', NULL, 'HIG-CHAMP-AV', 'Champú Avena para Perros y Gatos',
         'champu-avena-para-perros-y-gatos-hig-champ-av', 'Para pieles sensibles', 27.50, 14, TRUE)
     ) AS p (category_slug, brand_slug, sku, name, slug, description, price, stock, active)
JOIN categories c ON c.slug = p.category_slug
-- LEFT JOIN: los productos sin marca tienen brand_slug NULL
LEFT JOIN brands b ON b.slug = p.brand_slug
ON CONFLICT (sku) DO NOTHING;

-- --- Especies de cada producto ---

INSERT INTO product_species (product_id, species_id)
SELECT p.id, s.id
FROM (VALUES
        ('RC-MED-AD-15', 'DOG'),
        ('RC-MINI-PUP-3', 'DOG'),
        ('PP-AD-LG-15', 'DOG'),
        ('PED-AD-21', 'DOG'),
        ('RC-OLD-1', 'DOG'),
        ('RC-IND-4', 'CAT'),
        ('WHI-AD-POL-1', 'CAT'),
        ('HIL-KIT-1', 'CAT'),
        ('VK-MENU-CON', 'RABBIT'),
        ('PED-DENTA-7', 'DOG'),
        ('VK-STICK-AVE', 'BIRD'),
        ('KONG-CL-M', 'DOG'),
        ('TOY-RAT-3', 'CAT'),
        ('TOY-PEL-GOMA', 'DOG'),
        ('TOY-PEL-GOMA', 'CAT'),
        ('ACC-COM-INOX', 'DOG'),
        ('ACC-COM-INOX', 'CAT'),
        ('ACC-ARN-M', 'DOG'),
        ('HIG-ARENA-10', 'CAT'),
        ('HIG-CHAMP-AV', 'DOG'),
        ('HIG-CHAMP-AV', 'CAT')
     ) AS ps (sku, species_code)
JOIN products p ON p.sku = ps.sku
JOIN species s ON s.code = ps.species_code
ON CONFLICT (product_id, species_id) DO NOTHING;
