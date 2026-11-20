# Plataforma SaaS - Sistema de Gestión de Suscripciones

[![CI](https://github.com/Luis-matador/Plataforma-Saas/actions/workflows/ci.yml/badge.svg)](https://github.com/Luis-matador/Plataforma-Saas/actions/workflows/ci.yml)

## Descripción

Core de una plataforma SaaS desarrollada con **Spring Boot 4**, **JPA/Hibernate**, **Thymeleaf** y **MySQL**. El sistema permite:

- Registro de usuarios con selección de país (para cálculo de impuestos)
- Elección de plan de suscripción (Basic, Premium, Enterprise)
- **Facturación automática cada 30 días** con impuestos según país
- **Prorrateo automático** al cambiar de un plan barato a uno más caro
- Gestión de métodos de pago (tarjeta, PayPal, transferencia)
- Panel de administración con auditoría de cambios (Hibernate Envers)
- **Validación de formularios** con Bean Validation + feedback visual en Thymeleaf
- Contraseñas cifradas con **BCrypt**
- Pruebas unitarias completas (79 tests) con JUnit 5 y Mockito

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 4.0.2 | Framework |
| Spring Data JPA | - | Persistencia |
| Hibernate Envers | - | Auditoría (@Audited) |
| Thymeleaf | - | Vistas |
| Bean Validation | Jakarta | Validación de DTOs |
| MySQL | 8.x+ | Base de datos |
| Lombok | - | Reducción boilerplate |
| Spring Security Crypto | - | BCrypt para contraseñas |
| JUnit 5 + Mockito | - | Testing |
| Bootstrap 5 | 5.3.0 | UI (CDN) |

---

## Diagrama Entidad-Relación

```
┌─────────────┐    1    ┌──────────────┐    *    ┌──────────────┐
│   USUARIO   │────────│  SUSCRIPCION  │────────│   FACTURA    │
│─────────────│    *   │──────────────│        │──────────────│
│ id          │        │ id           │        │ id           │
│ nombre      │        │ usuario_id   │◄─┐    │ suscripcion_id│
│ email       │        │ plan_id      │  │    │ usuario_id   │
│ password    │        │ estado       │  │    │ concepto     │
│ pais        │        │ fecha_inicio │  │    │ monto        │
│ fecha_reg   │        │ fecha_prox_f │  │    │ impuesto     │
│ es_admin    │        │ @Audited     │  │    │ monto_total  │
└──────┬──────┘        └──────┬───────┘  │    │ tasa_impuesto│
       │ 1                     │ *        │    │ es_prorrateo │
       │                       │ 1        │    │ estado       │
┌──────┴──────┐        ┌──────┴───────┐  │    └──────────────┘
│   PERFIL    │        │    PLAN      │  │
│─────────────│        │──────────────│  │
│ id          │        │ id           │  │
│ telefono    │        │ nombre       │  │
│ direccion   │        │ nivel (ENUM) │  │
│ usuario_id  │        │ precio_mens  │  │
└─────────────┘        │ descripcion  │  │
                       └──────┬───────┘  │
                              │ 1        │
                    ┌─────────┴────────┐ │
                    │PLAN_CARACTERISTICA│ │  (tabla intermedia @ManyToMany explícita)
                    │──────────────────│ │
                    │ id               │ │
                    │ plan_id (FK)     │ │
                    │ caracteristica_id│ │
                    │ limite           │ │
                    │ valor            │ │
                    │ activa           │ │
                    └─────────┬────────┘ │
                              │ *        │
                    ┌─────────┴────────┐ │
                    │ CARACTERISTICA   │ │
                    │──────────────────│ │
                    │ id               │ │
                    │ nombre           │ │
                    │ descripcion      │ │
                    │ icono            │ │
                    └──────────────────┘ │
                                         │
┌──────────────────────────┐             │
│  METODO_PAGO (Herencia)  │─────────────┘
│──────────────────────────│  * (por usuario)
│ id                       │
│ tipo_pago (ENUM)         │
│ usuario_id               │
│ activo                   │
├──────────────────────────┤
│ TARJETA: numero, titular │
│ PAYPAL: email_paypal     │
│ TRANSFERENCIA: cuenta    │
└──────────────────────────┘
```

### Relación @ManyToMany con tabla intermedia explícita

La relación entre `Plan` y `Caracteristica` se implementa mediante la entidad intermedia `PlanCaracteristica`, que permite almacenar atributos adicionales (límite, valor, activa) por cada asociación:

```
Plan (1) ──── (*) PlanCaracteristica (*) ──── (1) Caracteristica
```

Con `@UniqueConstraint` sobre `(plan_id, caracteristica_id)` para evitar duplicados.

### Enums del Sistema

| Enum | Valores |
|---|---|
| `EstadoSuscripcion` | ACTIVA, CANCELADA, MOROSA |
| `NivelPlan` | BASIC (1), PREMIUM (2), ENTERPRISE (3) |
| `TipoPago` | TARJETA, PAYPAL, TRANSFERENCIA |
| `EstadoFactura` | PENDIENTE, PAGADA, VENCIDA, ANULADA |

---

## Arquitectura y Patrones

### Capa DTO + Bean Validation
Los controladores **no reciben ni construyen entidades JPA**. Se utilizan DTOs (Data Transfer Objects) con anotaciones de validación Jakarta:

| DTO | Uso | Validaciones |
|---|---|---|
| `RegistroRequest` | Formulario de registro | `@NotBlank`, `@Email`, `@Size(min=6)` |
| `LoginRequest` | Formulario de login | `@Email`, `@NotBlank` |
| `TarjetaRequest` | Alta de tarjeta | `@Size(13,19)`, `@NotBlank`, `@Size(5)` |
| `PayPalRequest` | Alta de PayPal | `@Email` |
| `TransferenciaRequest` | Alta de transferencia | `@NotBlank` |

### Validación en formularios (Thymeleaf)
Cada formulario implementa el patrón completo de validación:
- `th:object="${dto}"` — binding del DTO
- `th:field="*{campo}"` — enlace bidireccional
- `th:classappend="${#fields.hasErrors('campo')} ? 'is-invalid'"` — marcado visual
- `th:errors="*{campo}"` — mensaje de error inline
- `@Valid` + `BindingResult` en el controlador

### Separación Controller → Service
- Los controllers solo manejan **IDs** (Long) y **DTOs**, nunca entidades JPA
- Toda la lógica de negocio, filtrados y validación de propiedad está en los Services
- Los services reciben IDs y buscan las entidades internamente

---

## Estructura del Proyecto

```
src/main/java/com/example/plataformaSaaS/
├── PlataformaSaaSApplication.java          # Aplicación principal + @EnableScheduling
├── config/
│   ├── DataInitializer.java                # Datos iniciales (planes, características, admin)
│   └── SecurityConfig.java                 # Bean PasswordEncoder (BCrypt)
├── controller/
│   ├── HomeController.java                 # Página de inicio
│   ├── RegistroController.java             # Registro y login (con DTOs + @Valid)
│   ├── DashboardController.java            # Panel del usuario
│   ├── SuscripcionController.java          # Gestión de suscripciones (sin entidades)
│   ├── FacturaController.java              # Facturas con filtros delegados al servicio
│   ├── PagoController.java                 # Métodos de pago y pagos (con DTOs + @Valid)
│   └── AdminController.java               # Panel de administración
├── dto/
│   ├── AuditoriaDTO.java                  # DTO para la vista de auditoría
│   ├── RegistroRequest.java               # DTO registro con validación
│   ├── LoginRequest.java                  # DTO login con validación
│   ├── TarjetaRequest.java               # DTO alta tarjeta con validación
│   ├── PayPalRequest.java                # DTO alta PayPal con validación
│   └── TransferenciaRequest.java         # DTO alta transferencia con validación
├── model/
│   ├── enums/
│   │   ├── EstadoSuscripcion.java
│   │   ├── EstadoFactura.java
│   │   ├── NivelPlan.java
│   │   └── TipoPago.java
│   ├── Usuario.java
│   ├── Perfil.java
│   ├── Plan.java                          # @OneToMany → PlanCaracteristica
│   ├── Caracteristica.java                # Entidad de características
│   ├── PlanCaracteristica.java            # Tabla intermedia ManyToMany explícita
│   ├── Suscripcion.java                   # @Audited (Envers)
│   ├── Factura.java
│   ├── MetodoPago.java                    # Herencia SINGLE_TABLE
│   ├── PagoTarjeta.java
│   ├── PagoPayPal.java
│   └── PagoTransferencia.java
├── repository/
│   ├── UsuarioRepository.java
│   ├── PlanRepository.java
│   ├── SuscripcionRepository.java
│   ├── FacturaRepository.java
│   ├── MetodoPagoRepository.java
│   ├── CaracteristicaRepository.java
│   └── PlanCaracteristicaRepository.java
└── service/
    ├── UsuarioService.java                # Registro desde DTO, login, BCrypt
    ├── PlanService.java
    ├── SuscripcionService.java            # Lógica de prorrateo + delegación por IDs
    ├── FacturaService.java                # Impuestos, pago, filtros delegados
    ├── MetodoPagoService.java             # CRUD con verificación de propiedad
    ├── ScheduledBillingService.java       # Facturación automática @Scheduled
    └── AuditoriaService.java              # Consultas Envers
```

---

## Lógica de Negocio Principal

### Facturación Automática (cada 30 días)
- `ScheduledBillingService` ejecuta diariamente a medianoche (`@Scheduled`)
- Busca suscripciones ACTIVAS con `fechaProximaFacturacion <= hoy`
- Genera factura con impuestos según el país del usuario
- Actualiza `fechaProximaFacturacion` a +30 días
- También ejecutable manualmente desde el Panel Admin

### Cálculo de Prorrateo (upgrade de plan)
Al cambiar de un plan barato a uno más caro:
```
diasRestantes = fechaProximaFacturacion - hoy
precioDiarioActual = precioActual / 30
precioDiarioNuevo = precioNuevo / 30
prorrateo = (precioDiarioNuevo - precioDiarioActual) × diasRestantes
```
Se genera una factura de prorrateo con los impuestos correspondientes.

### Impuestos por País
| País | Tasa |
|---|---|
| España | 21% |
| Argentina | 21% |
| México | 16% |
| Colombia | 19% |
| Chile | 19% |
| Perú | 18% |
| Francia | 20% |
| Reino Unido | 20% |
| Alemania | 19% |
| Estados Unidos | 0% |
| Otros | 10% (default) |

### Auditoría con Hibernate Envers
La entidad `Suscripcion` está anotada con `@Audited`. Cada vez que se crea, modifica o elimina una suscripción, Envers registra automáticamente:
- El estado de la entidad antes/después del cambio
- La fecha del cambio
- El número de revisión

---

## Planes de Suscripción

| Plan | Precio/mes | Características |
|---|---|---|
| **Basic** | 9.99€ | 5 proyectos, 1GB almacenamiento, Soporte email |
| **Premium** | 29.99€ | 25 proyectos, 10GB almacenamiento, Soporte prioritario, API |
| **Enterprise** | 99.99€ | Ilimitados, 100GB, Soporte 24/7, API, SSO, SLA 99.9% |

---

## Puesta en marcha

### 1. Iniciar XAMPP
1. Abrir **XAMPP Control Panel**
2. Pulsar **Start** en **Apache** y **MySQL**

### 2. Crear la base de datos
Abrir **phpMyAdmin** (http://localhost/phpmyadmin) o una terminal MySQL y ejecutar:
```sql
CREATE DATABASE plataformaSaas_db;
```

### 3. Ejecutar la aplicación
Desde la raíz del proyecto:
```bash
./mvnw spring-boot:run
```
> En Windows usar `.\mvnw.cmd spring-boot:run`

Las tablas se crean automáticamente al arrancar (`ddl-auto=update`).

### 4. Acceder
- **Aplicación:** http://localhost:8080
- **Admin:** admin@saas.com / admin123

### Ejecutar tests
```bash
./mvnw test
```

---

## Pruebas Unitarias (79 tests - JUnit 5)

### Resumen por Clase

| Clase de Test | Tests | Cobertura |
|---|---|---|
| `FacturaServiceTest` | 23 | Impuestos por país, generación de facturas, prorrateo, pago, anulación |
| `SuscripcionServiceTest` | 17 | Creación, cambio de plan, prorrateo, cancelación, estados |
| `PagoControllerTest` | 13 | Endpoints de pago, métodos de pago, validación DTOs |
| `MetodoPagoServiceTest` | 11 | CRUD tarjeta/PayPal/transferencia, búsqueda, eliminación |
| `UsuarioServiceTest` | 10 | Registro con BCrypt, login, búsquedas, enums |
| `ScheduledBillingServiceTest` | 4 | Facturación automática, manejo de errores |
| `PlataformaSaaSApplicationTests` | 1 | Context load con MySQL |
| **Total** | **79** | **Todas PASS ✅** |

### Tabla de Pruebas Realizadas

| # | Caso de Prueba | Clase de Test | Resultado | Corrección |
|---|---|---|---|---|
| 1 | Impuesto España 21% | FacturaServiceTest | ✅ PASS | - |
| 2 | Impuesto México 16% | FacturaServiceTest | ✅ PASS | - |
| 3 | Impuesto Argentina 21% | FacturaServiceTest | ✅ PASS | - |
| 4 | Impuesto Colombia 19% | FacturaServiceTest | ✅ PASS | - |
| 5 | Impuesto Chile 19% | FacturaServiceTest | ✅ PASS | - |
| 6 | Impuesto USA 0% | FacturaServiceTest | ✅ PASS | - |
| 7 | Impuesto país desconocido 10% | FacturaServiceTest | ✅ PASS | - |
| 8 | Impuesto sobre monto decimal | FacturaServiceTest | ✅ PASS | - |
| 9 | Factura renovación con impuestos (España) | FacturaServiceTest | ✅ PASS | - |
| 10 | Factura renovación sin impuestos (USA) | FacturaServiceTest | ✅ PASS | - |
| 11 | Factura de prorrateo | FacturaServiceTest | ✅ PASS | - |
| 12 | Factura se guarda en repositorio | FacturaServiceTest | ✅ PASS | - |
| 13 | Obtener tasa de impuesto | FacturaServiceTest | ✅ PASS | - |
| 14 | Crear suscripción genera primera factura | SuscripcionServiceTest | ✅ PASS | - |
| 15 | Crear suscripción duplicada falla | SuscripcionServiceTest | ✅ PASS | - |
| 16 | Upgrade Basic→Premium genera prorrateo | SuscripcionServiceTest | ✅ PASS | - |
| 17 | Downgrade Premium→Basic NO genera prorrateo | SuscripcionServiceTest | ✅ PASS | - |
| 18 | Prorrateo con 30 días restantes | SuscripcionServiceTest | ✅ PASS | - |
| 19 | Prorrateo con 15 días restantes | SuscripcionServiceTest | ✅ PASS | - |
| 20 | Prorrateo Basic→Enterprise 20 días | SuscripcionServiceTest | ✅ PASS | - |
| 21 | Prorrateo 0 días restantes | SuscripcionServiceTest | ✅ PASS | - |
| 22 | Cambiar al mismo plan falla | SuscripcionServiceTest | ✅ PASS | - |
| 23 | Cambiar plan suscripción cancelada falla | SuscripcionServiceTest | ✅ PASS | - |
| 24 | Cambiar plan suscripción inexistente falla | SuscripcionServiceTest | ✅ PASS | - |
| 25 | Cancelar suscripción correctamente | SuscripcionServiceTest | ✅ PASS | - |
| 26 | Marcar suscripción morosa | SuscripcionServiceTest | ✅ PASS | - |
| 27 | Obtener suscripción activa | SuscripcionServiceTest | ✅ PASS | - |
| 28 | Suscripción activa no existe | SuscripcionServiceTest | ✅ PASS | - |
| 29 | Facturación automática procesa vencidas | ScheduledBillingServiceTest | ✅ PASS | - |
| 30 | Facturación actualiza fecha próxima +30 | ScheduledBillingServiceTest | ✅ PASS | - |
| 31 | Sin suscripciones vencidas no genera facturas | ScheduledBillingServiceTest | ✅ PASS | - |
| 32 | Error en una no detiene las demás | ScheduledBillingServiceTest | ✅ PASS | - |
| 33 | Registro exitoso de usuario | UsuarioServiceTest | ✅ PASS | - |
| 34 | Registro falla con email duplicado | UsuarioServiceTest | ✅ PASS | - |
| 35 | Login exitoso | UsuarioServiceTest | ✅ PASS | - |
| 36 | Login falla password incorrecta | UsuarioServiceTest | ✅ PASS | - |
| 37 | Login falla email inexistente | UsuarioServiceTest | ✅ PASS | - |
| 38 | Buscar usuario por ID | UsuarioServiceTest | ✅ PASS | - |
| 39 | Buscar usuario por ID inexistente | UsuarioServiceTest | ✅ PASS | - |
| 40 | Buscar usuario por email | UsuarioServiceTest | ✅ PASS | - |
| 41 | Estados suscripción correctos | UsuarioServiceTest | ✅ PASS | - |
| 42 | Niveles de plan orden correcto | UsuarioServiceTest | ✅ PASS | - |
| 43 | Pagar factura pendiente correctamente | FacturaServiceTest | ✅ PASS | - |
| 44 | No pagar factura ya pagada | FacturaServiceTest | ✅ PASS | - |
| 45 | No pagar factura anulada | FacturaServiceTest | ✅ PASS | - |
| 46 | No pagar factura inexistente | FacturaServiceTest | ✅ PASS | - |
| 47 | tieneFacturasPendientes true | FacturaServiceTest | ✅ PASS | - |
| 48 | tieneFacturasPendientes false | FacturaServiceTest | ✅ PASS | - |
| 49 | obtenerUltimaFacturaPendienteId correcto | FacturaServiceTest | ✅ PASS | - |
| 50 | obtenerUltimaFacturaPendienteId null | FacturaServiceTest | ✅ PASS | - |
| 51 | anularFacturasPendientes cambia estado | FacturaServiceTest | ✅ PASS | - |
| 52 | anularFacturasPendientes sin facturas | FacturaServiceTest | ✅ PASS | - |
| 53 | Upgrade con pendientes anula y genera nueva | SuscripcionServiceTest | ✅ PASS | - |
| 54 | Downgrade con pendientes anula y genera nueva | SuscripcionServiceTest | ✅ PASS | - |
| 55 | GET /factura/{id}/pagar muestra vista | PagoControllerTest | ✅ PASS | - |
| 56 | GET /factura/{id}/pagar sin login redirige | PagoControllerTest | ✅ PASS | - |
| 57 | GET /factura/{id}/pagar factura no existe | PagoControllerTest | ✅ PASS | - |
| 58 | GET /factura/{id}/pagar factura otro usuario | PagoControllerTest | ✅ PASS | - |
| 59 | GET /factura/{id}/pagar factura no pendiente | PagoControllerTest | ✅ PASS | - |
| 60 | POST /factura/{id}/pagar exitoso | PagoControllerTest | ✅ PASS | - |
| 61 | POST /factura/{id}/pagar método otro usuario | PagoControllerTest | ✅ PASS | - |
| 62 | GET /metodos-pago lista métodos y DTOs | PagoControllerTest | ✅ PASS | - |
| 63 | POST /metodos-pago/tarjeta agrega tarjeta | PagoControllerTest | ✅ PASS | - |
| 64 | POST /metodos-pago/paypal agrega PayPal | PagoControllerTest | ✅ PASS | - |
| 65 | POST /metodos-pago/transferencia agrega transferencia | PagoControllerTest | ✅ PASS | - |
| 66 | POST /metodos-pago/eliminar propio | PagoControllerTest | ✅ PASS | - |
| 67 | POST /metodos-pago/eliminar ajeno falla | PagoControllerTest | ✅ PASS | - |
| 68 | Guardar tarjeta últimos 4 dígitos | MetodoPagoServiceTest | ✅ PASS | - |
| 69 | Guardar tarjeta corta | MetodoPagoServiceTest | ✅ PASS | - |
| 70 | Guardar tarjeta exacto 4 dígitos | MetodoPagoServiceTest | ✅ PASS | - |
| 71 | Guardar tarjeta invoca save | MetodoPagoServiceTest | ✅ PASS | - |
| 72 | Guardar PayPal correctamente | MetodoPagoServiceTest | ✅ PASS | - |
| 73 | Guardar transferencia bancaria | MetodoPagoServiceTest | ✅ PASS | - |
| 74 | Listar métodos con resultados | MetodoPagoServiceTest | ✅ PASS | - |
| 75 | Listar métodos vacío | MetodoPagoServiceTest | ✅ PASS | - |
| 76 | Buscar método por ID existente | MetodoPagoServiceTest | ✅ PASS | - |
| 77 | Buscar método por ID inexistente | MetodoPagoServiceTest | ✅ PASS | - |
| 78 | Eliminar método de pago | MetodoPagoServiceTest | ✅ PASS | - |
| 79 | Context loads (integración) | PlataformaSaaSApplicationTests | ✅ PASS | - |

### Cobertura de Tests
- **FacturaService** (23): Cálculo de impuestos por país, generación de facturas, prorrateo, pago/anulación, facturas pendientes
- **SuscripcionService** (17): Creación, cambio de plan con/sin pendientes, prorrateo, cancelación, estados
- **PagoController** (13): Endpoints REST, validación de DTOs, verificación de propiedad, flujos de error
- **MetodoPagoService** (11): CRUD tarjeta/PayPal/transferencia, búsquedas, almacenamiento seguro
- **UsuarioService** (10): Registro con BCrypt, login, búsquedas, enums
- **ScheduledBillingService** (4): Facturación automática, resiliencia ante errores
- **Integración** (1): Context load completo con MySQL

---

## Vistas (Thymeleaf + Bootstrap 5)

| Vista | Ruta | Descripción |
|---|---|---|
| Inicio | `/` | Landing page |
| Registro | `/registro` | Formulario de registro con país |
| Login | `/login` | Inicio de sesión |
| Dashboard | `/dashboard` | Panel con suscripción y facturas |
| Planes | `/planes` | Selección/cambio de plan |
| Facturas | `/facturas` | Facturas con filtros (fecha/monto) |
| Suscripción | `/suscripcion` | Detalle de suscripción |
| Admin Panel | `/admin/panel` | Dashboard administrativo |
| Admin Facturas | `/admin/facturas` | Todas las facturas del sistema |
| Pagar Factura | `/factura/{id}/pagar` | Pago de una factura pendiente |
| Métodos de Pago | `/metodos-pago` | Gestión de métodos de pago |
| Admin Panel | `/admin/panel` | Dashboard administrativo |
| Admin Facturas | `/admin/facturas` | Todas las facturas del sistema |
| Admin Auditoría | `/admin/auditoria` | Historial de cambios (Envers) |

---
