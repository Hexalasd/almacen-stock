-- Script de migración para actualizar bases de datos existentes
-- Se ejecuta manualmente cuando se actualiza la aplicación

-- Verificar si las columnas ya existen antes de agregarlas
-- SQLite no soporta ALTER TABLE IF NOT EXISTS para columnas

-- Agregar columnas de unidades a la tabla products si no existen
ALTER TABLE products ADD COLUMN purchase_unit TEXT;
ALTER TABLE products ADD COLUMN sale_unit TEXT;

-- Crear tabla categories si no existe
CREATE TABLE IF NOT EXISTS categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Agregar columna category_id a products si no existe
ALTER TABLE products ADD COLUMN category_id INTEGER;

-- Insertar categorías por defecto si no existen
INSERT OR IGNORE INTO categories (name) VALUES ('Alimentos'), ('Bebidas'), ('Limpieza'), ('Cuidado Personal'), ('Otros');

-- Actualizar productos existentes que tengan categoría como texto para usar category_id
-- Esto es una migración simple, puede requerir ajustes manuales
UPDATE products SET category_id = (SELECT id FROM categories WHERE name = products.category) 
WHERE products.category IS NOT NULL AND category_id IS NULL;
