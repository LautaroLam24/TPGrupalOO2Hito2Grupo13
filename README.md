# Epicentro Gourmet

**Grupo 13 — Orientación a Objetos 2 — UNLa — 2026**

Epicentro Gourmet es un sistema web para gestionar festivales gastronómicos, unidades de venta, personal, platos y pedidos. También ofrece liquidaciones de haberes, recibos en PDF y reportes de recaudación, canon y rentabilidad según el rol del usuario.

Repositorio: [LautaroLam24/TPGrupalOO2Hito2Grupo13](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13)

## 1. Integrantes

| Integrante | Usuario de GitHub |
|---|---|
| Lautaro Lamaita | [LautaroLam24](https://github.com/LautaroLam24) |
| Gabriel Mendieta | [GabrielMendieta798](https://github.com/GabrielMendieta798) |
| Facundo Simonetta | [facusimonetta](https://github.com/facusimonetta) |
| Mateo Salomon | [MateSalomon15](https://github.com/MateSalomon15) |

## 2. Diagrama de clases

[LINK_AL_DIAGRAMA]
(https://drive.google.com/file/d/17E4o7uPkyZVDVv4HFDbzgA6p935ehxfq/view?usp=sharing)

## 3. Tecnologías

- Java 17 y Spring Boot 3.3.12.
- Maven, con Maven Wrapper incluido.
- MySQL 8.
- Spring Data JPA con Hibernate (`ddl-auto: update`).
- Thymeleaf y Bootstrap 5.3.3 por CDN.
- Spring Security con BCrypt.
- Spring Validation.
- Spring Mail con Gmail SMTP.
- OpenPDF 1.3.39 para generar recibos de sueldo.

## 4. Cómo correr la aplicación

### Requisitos

- Java 17.
- MySQL 8.
- Maven o el wrapper incluido en el repositorio.

### Crear la base de datos

Ejecutar en MySQL:

```sql
CREATE DATABASE epicentro_gourmet;
```

No es necesario crear las tablas manualmente: Hibernate las crea o actualiza al iniciar la aplicación.

### Configurar las variables de entorno

La aplicación requiere estas variables:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/epicentro_gourmet
DB_USERNAME=tu_usuario_mysql
DB_PASSWORD=tu_password_mysql
MAIL_USERNAME=tu_cuenta@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx
```

No se deben versionar credenciales reales. `MAIL_PASSWORD` debe ser una [contraseña de aplicación de Google](https://myaccount.google.com/apppasswords), generada con la verificación en dos pasos habilitada. Tiene 16 caracteres y debe guardarse sin espacios. Sin credenciales de correo válidas, las operaciones de envío de mails fallan, aunque el resto de la aplicación puede utilizarse con la base correctamente configurada.

#### IntelliJ IDEA

1. Abrir **Run > Edit Configurations**.
2. Seleccionar la configuración de Spring Boot.
3. Agregar las cinco variables en **Environment variables**.
4. Aplicar los cambios y volver a iniciar la aplicación.

#### Visual Studio Code

Crear `secrets.env` en la raíz con las cinco variables anteriores. El archivo ya está contemplado en `.gitignore`.

En la configuración que inicia `EpicentrogourmetApplication`, usar `.vscode/launch.json` sin un bloque `env` con credenciales:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Epicentro Gourmet (local)",
      "request": "launch",
      "mainClass": "com.unla.epicentrogourmet.EpicentrogourmetApplication",
      "cwd": "${workspaceFolder}",
      "envFile": "${workspaceFolder}/secrets.env",
      "console": "integratedTerminal"
    }
  ]
}
```

### Ejecutar

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

En Windows:

```bat
mvnw.cmd spring-boot:run
```

Luego ingresar a [http://localhost:8080](http://localhost:8080).

### Datos de prueba

[`src/main/resources/data.sql`](src/main/resources/data.sql) se ejecuta en cada arranque porque la inicialización SQL está configurada en modo `always`. Sus sentencias usan `INSERT IGNORE`, por lo que no reemplazan filas existentes.

Si la base ya contiene datos cargados manualmente con los mismos IDs, puede ser conveniente recrearla antes de iniciar para evitar datos semilla incompletos o inconsistentes:

```sql
DROP DATABASE epicentro_gourmet;
CREATE DATABASE epicentro_gourmet;
```

Esta operación elimina todos los datos existentes.

## 5. Usuarios de prueba

Todos los usuarios incluidos actualmente en `data.sql` están habilitados.

| Email | Contraseña | Rol | Unidad de venta | Estado |
|---|---|---|---|---|
| `admin@epicentro.com` | `admin` | `ROLE_ADMIN` | No aplica | Habilitado |
| `responsable1@epicentro.com` | `responsable` | `ROLE_RESPONSABLE` | Ruedas de Fuego | Habilitado |
| `responsable2@epicentro.com` | `responsable` | `ROLE_RESPONSABLE` | La Carpa Criolla | Habilitado |
| `responsable3@epicentro.com` | `responsable` | `ROLE_RESPONSABLE` | El Rincón Dulce | Habilitado |
| `cajero@epicentro.com` | `empleado` | `ROLE_EMPLEADO` | Ruedas de Fuego | Habilitado |
| `cocinero@epicentro.com` | `empleado` | `ROLE_EMPLEADO` | Ruedas de Fuego | Habilitado |

## 6. Roles y funcionalidades

**Administrador.** Consulta festivales, unidades asociadas, rankings por recaudación y unidades con mayor canon. También registra nuevas unidades de venta y sus responsables.

**Responsable.** Administra empleados y platos de su unidad, calcula y envía liquidaciones, y consulta plato estrella, recaudación, canon y rentabilidad por festival o entre fechas.

**Empleado.** Consulta sus liquidaciones y descarga recibos en PDF, revisa su historial de festivales y unidades, y verifica si trabaja en la fecha actual.

**Cajero.** Es un empleado especializado. Además de las funciones comunes del empleado, registra pedidos para la unidad y confirma el cierre de caja de los pedidos de su sesión.

## 7. Casos de uso implementados

| ID | Caso de uso | Actor | Responsable | Pull Request |
|---|---|---|---|---|
| CU01 | Ver festivales en la página de inicio | Visitante | Mateo Salomon | [#4](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/4) |
| CU02 | Registrar unidad de venta con su responsable (por corrección de la precorrección, lo hace el Administrador) | Administrador | Gabriel Mendieta (corrección: Lautaro Lamaita) | [#3](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/3), [#6](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/6), [#8](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/8), [#23](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/23) |
| CU03 | Iniciar y cerrar sesión | Todos | Lautaro Lamaita | [#1](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/1) |
| CU04 | Ver festivales | Administrador | Lautaro Lamaita | [#14](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/14) |
| CU05 | Ver unidades de venta de un festival | Administrador | Lautaro Lamaita | [#15](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/15) |
| CU06 | Ranking de unidades por recaudación | Administrador | Lautaro Lamaita | [#16](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/16) |
| CU07 | Unidades con mayor canon | Administrador | Lautaro Lamaita | [#20](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/20) |
| CU08 | Alta de empleado con envío de credenciales por mail | Responsable | Facundo Simonetta (correcciones: Mateo Salomon, Lautaro Lamaita) | [#10](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/10), [#24](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/24), [#27](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/27) |
| CU09 | Modificar empleado | Responsable | Mateo Salomon | [#25](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/25) |
| CU10 | Baja lógica de empleado | Responsable | Mateo Salomon | [#19](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/19) |
| CU11 | Reactivar empleado dado de baja | Responsable | Mateo Salomon | [#21](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/21) |
| CU12 | Filtrar empleados activos | Responsable | Mateo Salomon | [#18](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/18) |
| CU13 | Alta, modificación y baja lógica de platos | Responsable | Gabriel Mendieta | [#17](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/17) |
| CU14 | Filtrar platos activos | Responsable | Gabriel Mendieta | [#22](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/22) |
| CU15 | Calcular canon de la unidad | Responsable | Lautaro Lamaita | [#29](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/29) |
| CU16 | Liquidación de haberes | Responsable | Facundo Simonetta | [#7](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/7), [#30](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/30) |
| CU17 | Reporte de recaudación de un festival | Responsable | Gabriel Mendieta | [#13](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/13) |
| CU18 | Rentabilidad neta de la unidad en un festival | Responsable | Facundo Simonetta | [#31](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/31) |
| CU19 | Rentabilidad neta entre dos fechas | Responsable | Facundo Simonetta | [#32](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/32) |
| CU20 | Plato estrella | Responsable | Gabriel Mendieta | [#12](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/12) |
| CU21 | Ver festivales y unidades en las que trabajó | Empleado | Mateo Salomon | [#26](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/26) |
| CU22 | Ver sueldo por festival y descargar recibo en PDF | Empleado | Facundo Simonetta | [#30](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/30) |
| CU23 | Ver si trabaja hoy y en qué unidad | Empleado | Mateo Salomon | [#28](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/28) |
| CU24 | Cerrar caja al finalizar el día | Cajero | Gabriel Mendieta | [#11](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/11) |
| CU25 | Alta de pedido | Cajero | Gabriel Mendieta | [#9](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/9) |

No son casos de uso: datos de prueba ([#2](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/2), Lautaro Lamaita), corrección de columna ([#5](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/5), Lautaro Lamaita) y mostrar la unidad del usuario en las vistas ([#27](https://github.com/LautaroLam24/TPGrupalOO2Hito2Grupo13/pull/27), Lautaro Lamaita).

## 8. Requisitos del grupo

- **Envío de mail:** alta de empleado en CU08 mediante `EmailService.enviarCredenciales`, y liquidaciones de CU16/CU22 mediante `EmailService.enviarLiquidacion`.
- **DTO de reporte:** se utilizan, entre otros, [`ReporteVentaDTO`](src/main/java/com/unla/epicentrogourmet/dto/ReporteVentaDTO.java), [`RankingUnidadDTO`](src/main/java/com/unla/epicentrogourmet/dto/RankingUnidadDTO.java), [`ReporteMayoresCanonDTO`](src/main/java/com/unla/epicentrogourmet/dto/ReporteMayoresCanonDTO.java), `ReporteCanonDTO` y `ReporteRentabilidadNetaDTO`.
- **Generación de PDF:** CU22 genera el recibo de sueldo con OpenPDF desde `LiquidacionPdfService`, lo adjunta al correo y permite descargarlo desde las vistas del empleado.

## 9. Validaciones de carga por integrante

| Validación | Dónde se aplica | Responsable |
|---|---|---|
| El código de la unidad tiene 10 caracteres alfanuméricos, con al menos una letra y un número, y no está repetido | `UnidadDeVentaService.registrarConResponsable` y `UnidadDeVenta.validarCodigo` | Lautaro Lamaita |
| El email del responsable no está repetido | `UnidadDeVentaService.registrarConResponsable` | Lautaro Lamaita |
| El empleado es mayor de edad | `PersonalService.alta` mediante `validarDatos` | Facundo Simonetta |
| El DNI del empleado no está repetido | `PersonalService.alta` | Facundo Simonetta |
| La fecha de ingreso no es futura | `PersonalService.alta` mediante `validarDatos` | Facundo Simonetta |
| Al modificar, el DNI no está repetido por otro empleado | `PersonalService.modificar` mediante `validarModificacion` | Mateo Salomon |
| Al modificar, el empleado sigue siendo mayor de edad | `PersonalService.modificar` mediante `validarModificacion` | Mateo Salomon |
| El precio de venta del plato es mayor que su costo de producción | `PlatoService.alta` y `PlatoService.modificar` mediante `validarDatos` | Gabriel Mendieta |
| No se repite el nombre de un plato activo en la misma unidad | `PlatoService.alta` y `PlatoService.modificar` mediante `validarNombreDisponible` | Gabriel Mendieta |
| El pedido se carga únicamente con la fecha actual | `PedidoService.registrar` | Gabriel Mendieta |
| El pedido sólo se carga si la unidad está en un festival activo y utiliza platos activos de esa unidad | `PedidoService.registrar` mediante `obtenerContextoAutorizado` y la validación de platos | Gabriel Mendieta |

## 10. Metodología de trabajo

- Una rama por caso de uso, creada desde `master`, con la convención principal `CUFuncionalidadNombreApellido`.
- Integración de cambios mediante Pull Request.
- Commits siguiendo Conventional Commits, con prefijos como `feat`, `fix`, `test`, `refactor` y `docs`.
- Se priorizan descripciones en castellano; el historial conserva algunos commits anteriores redactados en inglés.
