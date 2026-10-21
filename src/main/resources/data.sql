-- =====================================================================
-- Usuarios de prueba (email / contraseña):
--   admin@epicentro.com        / admin        -> ROLE_ADMIN
--   responsable1@epicentro.com / responsable  -> ROLE_RESPONSABLE (food truck "Ruedas de Fuego")
--   responsable2@epicentro.com / responsable  -> ROLE_RESPONSABLE (puesto "La Carpa Criolla")
--   cajero@epicentro.com       / empleado     -> ROLE_EMPLEADO (cajero en "Ruedas de Fuego")
--   cocinero@epicentro.com     / empleado     -> ROLE_EMPLEADO (cocinera en "Ruedas de Fuego")
-- =====================================================================

INSERT IGNORE INTO role (id, role) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_RESPONSABLE'),
(3, 'ROLE_EMPLEADO');

INSERT IGNORE INTO costo (id, costo_superficie, costo_montaje, plus_electricidad, sueldo_base, plus_antiguedad, plus_cocinero, plus_ayudante, plus_lavaplatos) VALUES
(1, 500, 10, 2000, 100000, 5000, 150000, 100000, 50000),
(2, 500, 10, 2000, 100000, 5000, 150000, 100000, 50000),
(3, 500, 10, 2000, 100000, 5000, 150000, 100000, 50000);

INSERT IGNORE INTO festival (id, nombre, fecha_inicio, fecha_fin, costo_id) VALUES
(1, 'Festival de la Pizza', '2026-03-01', '2026-03-20', 1),
(2, 'Festival de la Cerveza Artesanal', '2026-10-15', '2026-11-15', 2),
(3, 'Festival del Asado', '2026-12-01', '2026-12-31', 3);

INSERT IGNORE INTO `user` (id, username, password, enabled, role_id) VALUES
(1, 'admin@epicentro.com',        '$2a$10$ostV37i1O6w2sTdPHFOsqO7qUtcSZjzlNNCHCwDLGnRb0KJEK5K1q', true, 1),
(2, 'responsable1@epicentro.com', '$2a$10$338dRDW0NVw/OzHSKj/etO5.qmeu0qh.8LL.lBnDTDVkKfailSWN.', true, 2),
(3, 'responsable2@epicentro.com', '$2a$10$338dRDW0NVw/OzHSKj/etO5.qmeu0qh.8LL.lBnDTDVkKfailSWN.', true, 2),
(4, 'cajero@epicentro.com',       '$2a$10$jGms4uhd68vpBi6LRK20V./T58hov2mntfv072FnK8.37b7RVPKSS', true, 3),
(5, 'cocinero@epicentro.com',     '$2a$10$jGms4uhd68vpBi6LRK20V./T58hov2mntfv072FnK8.37b7RVPKSS', true, 3);

INSERT IGNORE INTO unidad_de_venta (id, codigo, nombre_comercial, superficie_m2, responsable_id) VALUES
(1, 'FTK0000001', 'Ruedas de Fuego', 12, 2),
(2, 'PDS0000001', 'La Carpa Criolla', 20, 3);

INSERT IGNORE INTO food_truck (id, patente, requiere_electricidad) VALUES
(1, 'AB123CD', true);

INSERT IGNORE INTO puesto_desarmable (id, cant_carpas, tiempo_montaje_min) VALUES
(2, 2, 90);

INSERT IGNORE INTO festival_x_unidad (festival_id, unidad_id) VALUES
(1, 1),
(2, 1),
(2, 2);

INSERT IGNORE INTO plato (id, nombre, precio_venta, costo_produccion, baja, unidad_id) VALUES
(1, 'Pizza muzzarella', 6000, 2500, false, 1),
(2, 'Fugazzeta',        6500, 2700, false, 1),
(3, 'Cerveza IPA',      3500, 1200, false, 1),
(4, 'Choripán',         4000, 1500, false, 2),
(5, 'Empanada de carne',1500,  600, false, 2),
(6, 'Provoleta',        5000, 2000, true,  2);

INSERT IGNORE INTO personal (id, nombre, apellido, dni, fecha_nacimiento, fecha_ingreso, baja, user_id, unidad_id) VALUES
(1, 'Juan',  'Pérez', 30111222, '1995-05-10', '2022-03-01', false, 4, 1),
(2, 'María', 'Gómez', 32444555, '1998-08-22', '2024-01-15', false, 5, 1);

INSERT IGNORE INTO cajero (id, turno) VALUES
(1, 'MANANA');

INSERT IGNORE INTO empleado_cocina (id, categoria, especialidad) VALUES
(2, 'COCINERO', 'Pizzas a la piedra');

INSERT IGNORE INTO pedido (id, fecha, total, unidad_id) VALUES
(1, '2026-03-05', 18000, 1),  -- Festival de la Pizza
(2, '2026-10-16', 15500, 1),  -- Festival de la Cerveza
(3, '2026-10-17', 13500, 1),
(4, '2026-10-18',  6000, 1),
(5, '2026-10-17', 18000, 2);

INSERT IGNORE INTO item_pedido (id, cantidad, plato_id, pedido_id) VALUES
(1, 3, 1, 1),   -- 3 x Pizza muzzarella      = 18000
(2, 2, 1, 2),   -- 2 x Pizza muzzarella      = 12000
(3, 1, 3, 2),   -- 1 x Cerveza IPA           =  3500
(4, 1, 2, 3),   -- 1 x Fugazzeta             =  6500
(5, 2, 3, 3),   -- 2 x Cerveza IPA           =  7000
(6, 1, 1, 4),   -- 1 x Pizza muzzarella      =  6000
(7, 3, 4, 5),   -- 3 x Choripán              = 12000
(8, 4, 5, 5);   -- 4 x Empanada de carne     =  6000