-- Datos de referencia: especies base y razas comunes (todos los entornos).
-- Cada especie tiene una raza genérica ("Mestizo" o "Común") para mascotas sin raza definida.
-- Las razas se asocian por species.code, nunca por id: el id puede variar entre entornos.

INSERT INTO species (code, name)
VALUES ('DOG', 'Perro'),
       ('CAT', 'Gato'),
       ('BIRD', 'Ave'),
       ('RABBIT', 'Conejo'),
       ('RODENT', 'Roedor'),
       ('FISH', 'Pez'),
       ('REPTILE', 'Reptil');

INSERT INTO breeds (species_id, name)
SELECT s.id, b.name
FROM species s
         JOIN (VALUES ('DOG', 'Mestizo'),
                      ('DOG', 'Beagle'),
                      ('DOG', 'Bulldog Francés'),
                      ('DOG', 'Bulldog Inglés'),
                      ('DOG', 'Chihuahua'),
                      ('DOG', 'Cocker Spaniel'),
                      ('DOG', 'Dachshund'),
                      ('DOG', 'Golden Retriever'),
                      ('DOG', 'Husky Siberiano'),
                      ('DOG', 'Labrador Retriever'),
                      ('DOG', 'Pastor Alemán'),
                      ('DOG', 'Perro sin pelo del Perú'),
                      ('DOG', 'Pitbull'),
                      ('DOG', 'Pomerania'),
                      ('DOG', 'Poodle'),
                      ('DOG', 'Pug'),
                      ('DOG', 'Rottweiler'),
                      ('DOG', 'Schnauzer'),
                      ('DOG', 'Shih Tzu'),
                      ('DOG', 'Yorkshire Terrier'),
                      ('CAT', 'Común'),
                      ('CAT', 'Angora'),
                      ('CAT', 'Bengalí'),
                      ('CAT', 'British Shorthair'),
                      ('CAT', 'Maine Coon'),
                      ('CAT', 'Persa'),
                      ('CAT', 'Ragdoll'),
                      ('CAT', 'Siamés'),
                      ('CAT', 'Sphynx'),
                      ('BIRD', 'Común'),
                      ('RABBIT', 'Común'),
                      ('RODENT', 'Común'),
                      ('FISH', 'Común'),
                      ('REPTILE', 'Común')) AS b (species_code, name)
              ON b.species_code = s.code;
