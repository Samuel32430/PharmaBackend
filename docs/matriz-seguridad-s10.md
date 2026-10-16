# Matriz de Pruebas de Seguridad — Sesión 10
**Asignatura:** Lenguaje de Programación II (Ciclo IV - 2026-2)  
**Estudiante:** Samuel Vilca  
**Docente:** Reyna Barreto Benjamin David  
**Proyecto:** PharmaBackend — *Reto 02: Seguridad JWT y Gestión de Usuarios*  
**Rama:** `feature/seguridad-pruebas-vilca`

---

## Cuadro Consolidado de la Matriz de Pruebas (28 Casos)

| ID | Grupo | Usuario / Token | Petición | Resultado Esperado (Código y Mensaje) | Resultado Obtenido | Estado | Evidencia |
|---|---|---|---|---|---|:---:|---|
| **A-01** | Autenticación | — (Sin token) | `POST /api/v1/auth/login` con credenciales de admin, vendedor, almacen y supervisor | `200 OK`; cada respuesta retorna JWT, roles y permisos; supervisor devuelve la unión de `VENDEDOR` y `ALMACENERO` | `200 OK`; tokens emitidos con roles y permisos disjuntos y combinados | **PASA** | `A-01_postman.png` |
| **A-02** | Autenticación | — (Sin token) | `POST /api/v1/auth/login` con usuario inexistente y luego con contraseña errónea | `401 Unauthorized` con el mismo mensaje exacto: `"Usuario o contraseña incorrectos"` | `401 Unauthorized`; mensaje uniforme en ambos escenarios evitando enumeración | **PASA** | `A-02_postman.png` |
| **A-03** | Autenticación | — (Sin token) | `POST /api/v1/auth/login` con usuario `inactivo` y contraseña correcta | `401 Unauthorized` con `"Usuario o contraseña incorrectos"` (no revela estado) | `401 Unauthorized`; respuesta idéntica a credenciales inválidas | **PASA** | `A-03_postman.png` |
| **A-04** | Autenticación | — (Sin token) | `POST /api/v1/auth/login` con usuario en mayúsculas (`VENDEDOR`) | Autenticación permitida de forma case-insensitive (`200 OK`) según regla de usabilidad | `200 OK`; token emitido con sub en minúsculas normalizado | **PASA** | `A-04_postman.png` |
| **A-05** *(Propio)* | Autenticación | — (Sin token) | `POST /api/v1/auth/login` enviando `username: ""` y `password: ""` vacíos | `400 Bad Request` indicando campos obligatorios en `validationErrors` | `400 Bad Request`; validaciones `@NotBlank` interceptadas por `GlobalExceptionHandler` | **PASA** | `A-05_postman.png` |
| **T-01** | Token | Token sin prefijo Bearer | `GET /api/v1/productos` con cabecera `Authorization: <token>` | `401 Unauthorized`; `"Debe iniciar sesión para acceder a este recurso"` | `401 Unauthorized`; filtro ignora header malformado y entryPoint rechaza | **PASA** | `T-01_postman.png` |
| **T-02** | Token | Token alterado en payload | `GET /api/v1/productos` con un carácter cambiado en base64 del payload | `401 Unauthorized`; `"El token no es válido"` | `401 Unauthorized`; falla validación criptográfica de firma HMAC | **PASA** | `T-02_postman.png` |
| **T-03** | Token | Token firmado con otra clave | `GET /api/v1/productos` usando token generado con secreto distinto | `401 Unauthorized`; `"El token no es válido"` | `401 Unauthorized`; signature mismatch interceptado por JJWT | **PASA** | `T-03_postman.png` |
| **T-04** | Token | Token expirado | `GET /api/v1/productos` con token vencido (`expiracion-minutos: 1`) | `401 Unauthorized`; `"El token ha expirado. Inicie sesión nuevamente"` | `401 Unauthorized`; `ExpiredJwtException` capturada y mapeada | **PASA** | `T-04_postman.png` |
| **T-05** | Token | Token Vendedor | `GET /api/v1/auth/me` y decodificación en `jwt.io` | Claims `sub`, `authorities`, `iat`, `exp` íntegros; sin passwords ni hashes | Datos correctos; expiración fijada a 3600 segundos; sin datos confidenciales | **PASA** | `T-05_postman.png` |
| **T-06** *(Propio)* | Token | Sin encabezado | `GET /api/v1/ventas` sin cabecera `Authorization` | `401 Unauthorized`; `"Debe iniciar sesión para acceder a este recurso"` | `401 Unauthorized` retornado por `RestAuthenticationEntryPoint` | **PASA** | `T-06_postman.png` |
| **P-01** | Permisos | Token Vendedor | `POST`, `PUT`, `DELETE` sobre `/api/v1/categorias` | `403 Forbidden` en las tres peticiones (requiere `CATALOGO_ESCRIBIR`) | `403 Forbidden` con `"No tiene permisos para realizar esta operación"` | **PASA** | `P-01_postman.png` |
| **P-02** | Permisos | Token Almacenero | `GET /api/v1/clientes` y `POST /api/v1/ventas` | `403 Forbidden` en ambas peticiones por falta de permisos de clientes/ventas | `403 Forbidden` bloqueado por filtros de `SecurityConfig` | **PASA** | `P-02_postman.png` |
| **P-03** | Permisos | Token Vendedor / Almacenero | `GET /api/v1/reportes/productos-mas-vendidos` | `403 Forbidden` disparado por `@PreAuthorize("hasAuthority('REPORTES_VER')")` | `403 Forbidden` capturado por `GlobalExceptionHandler` | **PASA** | `P-03_postman.png` |
| **P-04** | Permisos | Token Admin | Petición sobre cada módulo (`/categorias`, `/clientes`, `/ventas`, `/usuarios`) | Códigos `2xx` en todas las rutas | `200 OK` y `201 Created` en todos los módulos evaluados | **PASA** | `P-04_postman.png` |
| **P-05** *(Propio)* | Permisos | Token Vendedor | `POST /api/v1/usuarios` intentando registrar usuario | `403 Forbidden` (exige `USUARIOS_ADMINISTRAR`) | `403 Forbidden`; acceso restringido a rol ADMIN | **PASA** | `P-05_postman.png` |
| **U-01** | Usuarios | Token Admin | `POST /api/v1/usuarios` con clave `"abcdefgh"` y luego con `rolIds: []` | `400 Bad Request` en ambos casos (validaciones `@Pattern` y `@NotEmpty`) | `400 Bad Request` con mapa de errores de validación estructurados | **PASA** | `U-01_postman.png` |
| **U-02** | Usuarios | Token Admin | `POST /api/v1/usuarios` empleado duplicado y luego alta de María Huamán | `409 Conflict` (relación 1:1) y luego `201 Created` con hash BCrypt en BD | `409 Conflict` en duplicado; `201 Created` exitoso para nueva usuaria | **PASA** | `U-02_postman.png` |
| **U-03** *(Propio)* | Usuarios | Token Admin | `POST /api/v1/usuarios` con contraseña > 72 bytes en UTF-8 | `409 Conflict`; `"La contraseña no puede superar 72 bytes"` (límite BCrypt) | `409 Conflict` evitando truncamiento silencioso del algoritmo | **PASA** | `U-03_postman.png` |
| **C-01** | CORS | Origen `http://localhost:4200` | Petición preflight `OPTIONS /api/v1/productos` | `200 OK` con cabeceras CORS; petición real `200` o `401` sin bloqueo CORS | `200 OK`; headers `Access-Control-Allow-Origin` presentes y correctos | **PASA** | `C-01_postman.png` |
| **B-01** | Gestión Claves | Token Vendedor | `PUT /api/v1/auth/password` con `passwordActual` correcta y nueva válida | `204 No Content`; nueva clave hasheada en BD; log INFO sin contraseñas | `204 No Content`; login posterior exitoso con la nueva credencial | **PASA** | `B-01_postman.png` |
| **B-02** | Gestión Claves | Token Vendedor | `PUT /api/v1/auth/password` con `passwordActual` errónea | `409 Conflict`; `"La contraseña actual no es correcta"` | `409 Conflict` devuelto por validación `passwordEncoder.matches()` | **PASA** | `B-02_postman.png` |
| **B-03** | Gestión Claves | Token Vendedor | `PUT /api/v1/auth/password` con `passwordNueva` igual a la actual | `409 Conflict`; `"La nueva contraseña debe ser distinta de la actual"` | `409 Conflict` impidiendo reutilización redundante de clave | **PASA** | `B-03_postman.png` |
| **B-04** | Gestión Claves | Token Vendedor | `PUT /api/v1/auth/password` con clave nueva menor a 8 caracteres o sin dígito | `400 Bad Request` indicando errores en formato de contraseña | `400 Bad Request` con mensajes `@Size` y `@Pattern` | **PASA** | `B-04_postman.png` |
| **B-05** | Asignación Roles | Token Admin | `PUT /api/v1/usuarios/2/roles` con `rolIds: [2, 3]` | `200 OK`; `UsuarioResponseDTO` con roles actualizados; permisos aplicados | `200 OK`; asignación de roles `VENDEDOR` y `ALMACENERO` reflejada | **PASA** | `B-05_postman.png` |
| **B-06** | Asignación Roles | Token Admin | `PUT /api/v1/usuarios/2/roles` con `rolIds: []` y luego `rolIds: [2, 99]` | `400 Bad Request` ante lista vacía; `404 Not Found` ante rol inexistente | `400 Bad Request` y `404 Not Found` en cada caso respectivo | **PASA** | `B-06_postman.png` |
| **B-07** | Asignación Roles | Token Admin | `PUT /api/v1/usuarios/1/roles` (Admin retira a sí mismo su rol ADMIN) | `409 Conflict`; `"Un administrador no puede retirarse a sí mismo el rol ADMIN"` | `409 Conflict`; protección de superusuario auto-contenido garantizada | **PASA** | `B-07_postman.png` |
| **B-08** | Estado Cuentas | Token Admin | `PATCH /api/v1/usuarios/2/estado` con `false`; probar token previo de vendedor | `200 OK` en desactivación; token previo recibe `401 («El usuario está deshabilitado»)` | `200 OK` y `401 Unauthorized` inmediato; auto-desactivación de Admin rechazada (409) | **PASA** | `B-08_postman.png` |

---
*Fin de la Matriz de Pruebas de Seguridad.*
