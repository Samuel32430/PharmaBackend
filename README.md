# PharmaBackend

Backend del sistema integral de farmacia **PharmaSoft**, desarrollado con **Spring Boot 4**, **Java 21 LTS**, **Spring Data Redis**, **Spring Security** y base de datos **Oracle 23ai**.

---

## Despliegue en producción

### 1. Diagrama de la Arquitectura

```text
+-------------------------------------------------------+
|             Navegadores Web / Terminales POS          |
|                 (Sucursal 1, 2 y 3)                   |
+---------------------------+---------------------------+
                            |
                            | HTTP / HTTPS (Puerto 8081 / 443)
                            v
+-------------------------------------------------------+
|                  Nginx 1.27 Alpine                    |
|              (Servidor Web & Proxy Inverso)           |
|  - Archivos SPA estáticos con hash inmutables (1 año) |
|  - index.html revalidado siempre (no-cache)           |
|  - Proxy inverso hacia el API: /api/*                 |
+---------------------------+---------------------------+
                            |
                            | HTTP Interno (Puerto 8080)
                            v
+-------------------------------------------------------+
|                    PharmaBackend                      |
|            (Spring Boot 4 · Java 21 LTS)              |
|  - Filtro MDC con RequestId (X-Request-Id)            |
|  - Endpoints protegidos con JWT y roles RBAC          |
|  - Endpoints de Actuator (/health, /metrics)          |
+---------------+-----------------------+---------------+
                |                       |
                | Puerto 1521 (SQL/TCP) | Puerto 6379 (RESP)
                v                       v
+-------------------------------+   +-------------------------------+
|      Oracle Database 23ai     |   |         Redis 7 Alpine        |
|    (Persistencia Principal)   |   |   (Caché en Memoria y TTL)    |
+-------------------------------+   +-------------------------------+
```

### 2. Variables de Entorno Necesarias

Las siguientes variables de entorno deben suministrarse en el entorno productivo (sin exponer credenciales en repositorios):

| Variable de Entorno | Propósito | Ejemplo / Tipo |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Perfil de configuración activo de Spring Boot | `prod` |
| `SPRING_DATASOURCE_URL` | Cadena JDBC de conexión al clúster de Oracle Database | `jdbc:oracle:thin:@//db-prod.internal:1521/FREEPDB1` |
| `SPRING_DATASOURCE_USERNAME` | Usuario del esquema de base de datos | `c##pharmasoft_prod` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña cifrada de la base de datos | *(secreto)* |
| `JWT_SECRET` | Llave secreta para firma criptográfica HMAC-SHA256 | *(cadena aleatoria de 256 bits)* |
| `JWT_EXPIRATION` | Tiempo de vida del token JWT en milisegundos | `86400000` (24 horas) |
| `REDIS_HOST` | Host o nombre de servicio del nodo Redis central | `redis-prod.internal` |
| `REDIS_PORT` | Puerto de enlace hacia la instancia de Redis | `6379` |
| `SERVER_PORT` | Puerto de escucha HTTP del backend de Spring Boot | `8080` |

### 3. Pasos para Construir, Desplegar y Rollback

#### Construcción y Despliegue
1. Clonar el repositorio y posicionarse en la rama de versión estable:
   ```bash
   git checkout main
   ```
2. Compilar y empaquetar el backend con el wrapper oficial de Maven:
   ```bash
   ./mvnw clean package -DskipTests
   ```
3. Ejecutar el artefacto jar generado cargando el perfil de producción:
   ```bash
   java -jar target/PharmaBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```
4. Verificar el arranque exitoso consultando el endpoint de salud:
   ```bash
   curl -I http://localhost:8080/actuator/health
   ```

#### Procedimiento de Rollback (Vuelta Atrás)
1. Detener el proceso del backend en caso de degradación o fallo crítico.
2. Restaurar la versión anterior previamente archivada (`PharmaBackend-release-prev.jar`):
   ```bash
   cp target/backup/PharmaBackend-release-prev.jar target/PharmaBackend-0.0.1-SNAPSHOT.jar
   systemctl restart pharmabackend
   ```
3. Verificar que `/actuator/health` devuelva `"status": "UP"`.

### 4. Lista de Buenas Prácticas de la Sesión

| N.º | Buena Práctica Evaluada | Estado en PharmaSoft | Acción Pendiente / Justificación |
| :---: | :--- | :---: | :--- |
| 1 | Carga diferida de rutas (`Lazy Loading`) | **Cumple** | Rutas modularizadas bajo demanda con `loadComponent` y `loadChildren`. |
| 2 | Componentes pesados diferidos (`@defer`) | **Cumple** | Reporte de más vendidos y componente de aviso de sesión diferidos. |
| 3 | Presupuestos estrictos de tamaño (*budgets*) | **Cumple** | Límites configurados en `angular.json` (warning 350 kB, error 400 kB). |
| 4 | Políticas de caché en Nginx | **Cumple** | `index.html` revalidado (`no-cache`), estáticos con hash inmutables a 1 año. |
| 5 | Prevención de caché del navegador en API REST | **Cumple** | Spring Security inyecta cabeceras `no-cache, no-store, max-age=0, must-revalidate`. |
| 6 | Caché distribuida en memoria con Redis | **Cumple** | Implementado para categorías y productos con serialización JSON. |
| 7 | Tolerancia a fallos de caché (*Fallback*) | **Cumple** | `CacheErrorHandler` en `CacheConfig` redirige consultas a Oracle si Redis cae. |
| 8 | Invalidación estricta con `@CacheEvict` | **Cumple** | `@CacheEvict(allEntries=true)` en creación, edición, borrado y registro de ventas. |
| 9 | Trazabilidad correlacionada con `X-Request-Id` | **Cumple** | Filtro de entrada inyecta UUID en MDC, logs y cabecera HTTP de respuesta. |
| 10 | Inclusión de `requestId` en respuestas de error | **Cumple** | `ErrorResponseDTO` incluye `requestId` consumido por la SPA para soporte técnico. |
| 11 | Auditoría de confidencialidad en logs | **Cumple** | Logs limpios de contraseñas, tokens JWT completos y secretos. |
| 12 | Endpoints de salud y métricas asegurados | **Cumple** | `/actuator/health` y `/actuator/metrics` protegidos bajo autorización de rol ADMIN. |

### 5. Recomendaciones Priorizadas para los 3 Locales de la Botica

1. **Cifrado TLS/HTTPS en el Frontend y Terminación SSL:**
   Configurar certificados TLS (Let's Encrypt o certificado corporativo) en Nginx. Dado que los tres locales operan a través de enlaces de red externos, el cifrado extremo a extremo es mandatorio para proteger las credenciales de los empleados y los datos personales y de salud de los clientes.
2. **Instancia Centralizada de Redis con Replicación y Persistencia (AOF/RDB):**
   Las tres sucursales deben operar sobre el mismo catálogo de productos y precios. Se recomienda desplegar un único servidor Redis central con persistencia activa (Append-Only File) para asegurar que una venta realizada en el Local 1 descuente el stock e invalide la caché de inmediato para los Locales 2 y 3.
3. **Monitoreo Proactivo y Alertas Tempranas con Actuator:**
   Integrar un sistema de sondeo automático (ej. Prometheus o Uptime Kuma) que consulte periódicamente `/actuator/health`. En caso de que el componente de base de datos o Redis pase a estado `DOWN`, el sistema debe enviar una notificación inmediata (Slack/Telegram) al administrador antes de que los cajeros reporten la incidencia.
