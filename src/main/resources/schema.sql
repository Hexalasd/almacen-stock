-- Script de inicialización de base de datos (SQLite)
-- Nota: se asume que se ejecuta con permisos suficientes para crear tablas.

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,
  role TEXT NOT NULL, -- ADMIN, INVENTORY, SELLER
  full_name TEXT,
  active INTEGER DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  category TEXT,
  supplier TEXT,
  location TEXT,
  purchase_price REAL,
  sale_price REAL,
  current_stock INTEGER DEFAULT 0,
  min_stock_alert INTEGER DEFAULT 5,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_movements (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  type TEXT NOT NULL, -- ENTRY, EXIT, ADJUSTMENT
  reason TEXT,
  user_id INTEGER,
  timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS alerts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  type TEXT NOT NULL, -- LOW_STOCK, OUT_OF_STOCK
  message TEXT,
  resolved INTEGER DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Usuario admin por defecto (password en texto plano por ahora)
INSERT OR IGNORE INTO users (username, password, role, full_name, active)
VALUES ('admin', 'admin123', 'ADMIN', 'Administrador', 1);

COMMIT;
