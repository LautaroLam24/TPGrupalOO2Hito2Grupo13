-- ===== Roles =====
INSERT IGNORE INTO role (role) VALUES ('ROLE_ADMIN'), ('ROLE_RESPONSABLE'), ('ROLE_EMPLEADO');

-- ===== Admin de prueba: admin@epicentro.com / admin =====
INSERT IGNORE INTO `user` (username, password, enabled, role_id)
SELECT 'admin@epicentro.com', '$2a$10$ostV37i1O6w2sTdPHFOsqO7qUtcSZjzlNNCHCwDLGnRb0KJEK5K1q', true, id
FROM role WHERE role = 'ROLE_ADMIN';

-- ===== Empleado de prueba (cajero): cajero@epicentro.com / empleado =====
INSERT IGNORE INTO `user` (username, password, enabled, role_id)
SELECT 'cajero@epicentro.com', '$2a$10$jGms4uhd68vpBi6LRK20V./T58hov2mntfv072FnK8.37b7RVPKSS', true, id
FROM role WHERE role = 'ROLE_EMPLEADO';

INSERT IGNORE INTO personal (nombre, apellido, dni, fecha_nacimiento, fecha_ingreso, baja, user_id)
SELECT 'Juan', 'Pérez', 30111222, '1995-05-10', '2022-03-01', false, id
FROM `user` WHERE username = 'cajero@epicentro.com';

INSERT IGNORE INTO cajero (id, turno)
SELECT id, 'MANANA' FROM personal WHERE dni = 30111222;