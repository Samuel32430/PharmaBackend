# Informe Técnico de Seguridad: Auditoría JWT, Control de Accesos y Gestión de Credenciales en PharmaBackend

**Universidad Peruana Unión (UPeU)**  
**Facultad de Ingeniería y Arquitectura · Escuela Profesional de Ingeniería de Sistemas**  
**Asignatura:** Lenguaje de Programación II · Ciclo IV · Semestre 2026-2  
**Unidad II:** Frontend SPA empresarial seguro  
**Sesión 10:** Pruebas de seguridad y gestión de contraseñas en PharmaBackend (Reto 02)  
**Estudiante:** Samuel Vilca  
**Docente:** Reyna Barreto Benjamin David  
**Fecha:** Octubre 2026  

---

## 1. Información General y Metadatos de la Entrega

* **Título de la Actividad:** Matriz de pruebas de seguridad, cambio de contraseña, asignación de roles y desactivación de usuarios.
* **Objetivo de la Sesión:** Verificar la autenticación JWT y la autorización por permisos de PharmaBackend contrastando resultados esperados y obtenidos, e implementar el cambio de contraseña con BCrypt, la asignación de roles (relación muchos a muchos) y la desactivación de usuarios respetando las reglas de seguridad.
* **Repositorio Backend:** `PharmaBackend` (Spring Boot 3 / 4, Java 21, Spring Security 6, JJWT 0.12.6).
* **Rama de Trabajo:** `feature/seguridad-pruebas-vilca` (creada a partir de `develop`).
* **Colección de Postman:** `postman/PharmaSoft-S10-Seguridad.json` con variable de entorno `baseUrl = http://localhost:8080/api/v1` y variables de tokens dinámicas.

---

## 2. Preparación del Entorno y Cuentas de Prueba

Para garantizar la reproducibilidad y validez de cada caso de prueba, se aprovisionó la base de datos relacional con los siguientes perfiles y precondiciones:

1. **Usuarios Base Registrados:**
   * **Administrador (`admin`):** Con rol `ADMIN` (ID 1). Posee los 7 permisos del sistema (`CATALOGO_LEER`, `CATALOGO_ESCRIBIR`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `VENTAS_LEER`, `VENTAS_REGISTRAR`, `USUARIOS_ADMINISTRAR`, `REPORTES_VER`).
   * **Vendedor (`vendedor`):** Con rol `VENDEDOR` (ID 2). Posee permisos de lectura de catálogo y clientes, y emisión de ventas (`VENTAS_REGISTRAR`).
   * **Almacenero (`almacen`):** Con rol `ALMACENERO` (ID 3). Posee permisos de lectura y escritura en catálogo (`CATALOGO_LEER`, `CATALOGO_ESCRIBIR`).
   * **Supervisora (`supervisor`):** Con roles múltiples `[VENDEDOR, ALMACENERO]`. Posee la unión sin duplicados de los permisos de ventas y catálogo.
   * **Usuario Inactivo (`inactivo`):** Con `estado = false`. Empleado registrado para evaluar rechazo estricto en autenticación.
2. **Parámetros Criptográficos:**
   * Algoritmo de Hashing: BCrypt con factor de costo 10 y límite de seguridad de 72 bytes.
   * Firma de Tokens JWT: Clave simétrica HMAC-SHA (HS384) de 384 bits con expiración predeterminada de 60 minutos (3600 segundos).

---

## Parte A: Matriz de Pruebas de Seguridad (8 Puntos)

Se ejecutó una batería de **28 casos de prueba** estructurados en 6 grupos funcionales: 16 casos base fijados en la guía académica, 4 casos propios de grupos independientes y 8 casos orientados a las nuevas funciones de administración de contraseñas y cuentas (B-01 a B-08).

El detalle consolidado con los datos de entrada, precondición, respuesta HTTP y estado se encuentra archivado en el documento [docs/matriz-seguridad-s10.md](matriz-seguridad-s10.md).

---

## Parte B: Nuevas Funcionalidades Implementadas (7 Puntos)

En cumplimiento de los requerimientos de la botica, se diseñaron e integraron los siguientes tres endpoints en la arquitectura multicapa de Spring Boot:

### B1. Cambio de Contraseña del Usuario Autenticado (`PUT /api/v1/auth/password`)
* **Controlador:** `AuthController.java` mapeando la ruta hacia `authService.cambiarPassword(...)`.
* **Seguridad:** Requiere token JWT válido (cualquier rol). El nombre de usuario se obtiene directamente desde `Authentication.getName()`, imposibilitando la suplantación de identidad mediante IDs falsificados en el cuerpo del mensaje.
* **Validaciones:**
  * Si la contraseña actual no coincide con el hash persistido (`passwordEncoder.matches`), se lanza `ReglaNegocioException("La contraseña actual no es correcta")` retornando **HTTP 409 Conflict**.
  * Si la nueva contraseña coincide con la actual, se lanza `ReglaNegocioException("La nueva contraseña debe ser distinta de la actual")` (**HTTP 409 Conflict**).
  * La nueva contraseña debe cumplir con una longitud de 8 a 72 caracteres y contener al menos una letra y un número (**HTTP 400 Bad Request** ante incumplimiento).
  * Se registra un evento en bitácora mediante `log.info("Cambio de contraseña exitoso para usuario={}", username)` omitiendo de manera estricta cualquier valor en texto plano o hash.
* **Respuesta Exitosa:** Código **HTTP 204 No Content**.

### B2. Asignación de Roles a un Usuario (`PUT /api/v1/usuarios/{id}/roles`)
* **Controlador:** `UsuarioController.java` protegido por `hasAuthority('USUARIOS_ADMINISTRAR')`.
* **Seguridad:** Solo ejecutable por usuarios con rol `ADMIN`.
* **Validaciones:**
  * Valida que la colección de roles no se encuentre vacía mediante `@NotEmpty Set<@NotNull Long> rolIds` (**HTTP 400 Bad Request**).
  * Valida la existencia del usuario objetivo y la existencia íntegra de todos los IDs de rol enviados en la base de datos (**HTTP 404 Not Found** si falta alguno).
  * **Regla Crítica de Auto-Protección:** Si el usuario que ejecuta la petición es el mismo administrador autenticado, se valida que dentro de los nuevos roles persista el rol `ADMIN`. Si intenta removerlo, se cancela la transacción lanzando `ReglaNegocioException("Un administrador no puede retirarse a sí mismo el rol ADMIN")` (**HTTP 409 Conflict**).
* **Mecanismo:** La entidad `Usuario` actualiza su colección mediante `usuario.setRoles(new HashSet<>(roles))`, permitiendo que Hibernate actualice atómicamente la tabla puente `usuario_roles`.
* **Respuesta Exitosa:** Código **HTTP 200 OK** con `UsuarioResponseDTO` conteniendo la lista renovada de roles y permisos.

### B3. Activación y Desactivación de Usuarios (`PATCH /api/v1/usuarios/{id}/estado`)
* **Controlador:** `UsuarioController.java` protegido por `hasAuthority('USUARIOS_ADMINISTRAR')`.
* **Seguridad:** Restringido a administradores del sistema.
* **Validaciones:**
  * Requiere el atributo booleano `@NotNull Boolean estado` (**HTTP 400 Bad Request**).
  * Valida la existencia del usuario en el repositorio (**HTTP 404 Not Found**).
  * **Regla Crítica de Auto-Protección:** Si el administrador intenta enviar `estado = false` contra su propio ID de usuario, la operación se aborta con `ReglaNegocioException("Un administrador no puede desactivar su propia cuenta")` (**HTTP 409 Conflict**).
* **Efecto Inmediato:** Gracias a que `JwtAuthenticationFilter` invoca a `UserDetailsService.loadUserByUsername()` en cada petición entrante verificando `usuario.isEnabled()`, en el instante en que una cuenta es desactivada, cualquier token previamente emitido queda revocado de inmediato respondiendo **HTTP 401 Unauthorized: «El usuario está deshabilitado»**.

---

## Parte C: Informe de Seguridad (3 Puntos)

### C1. Resumen Estadístico de Ejecución de Pruebas

| Grupo de Prueba | Total Casos | Casos que Pasan | Casos que Fallan | Tasa de Conformidad |
|---|:---:|:---:|:---:|:---:|
| **Autenticación (A-01 a A-05)** | 5 | 5 | 0 | 100.0 % |
| **Tokens JWT (T-01 a T-06)** | 6 | 6 | 0 | 100.0 % |
| **Permisos y Roles (P-01 a P-05)** | 5 | 5 | 0 | 100.0 % |
| **Gestión de Usuarios (U-01 a U-03)** | 3 | 3 | 0 | 100.0 % |
| **CORS (C-01)** | 1 | 1 | 0 | 100.0 % |
| **Nuevas Funcionalidades (B-01 a B-08)** | 8 | 8 | 0 | 100.0 % |
| **TOTAL GENERAL** | **28** | **28** | **0** | **100.0 %** |

**Diagnóstico Global:**  
La arquitectura de seguridad demuestra un comportamiento robusto ante vectores de ataque convencionales. Todos los accesos no autenticados o provistos de tokens ilegítimos son interceptados por `RestAuthenticationEntryPoint` (401), mientras que los intentos de escalamiento de privilegios o ejecución de acciones no asignadas al rol son contenidos por `RestAccessDeniedHandler` (403). Las reglas transaccionales de autoprotección del administrador previenen bloqueos catastróficos del sistema.

---

### C2. Ficha de Hallazgo Técnico Preventivo

#### Ficha de Hallazgo H-01
* **ID del Hallazgo:** H-01
* **Caso Relacionado:** U-03 / A-04 (Límite Criptográfico de BCrypt y Tratamiento de Caracteres UTF-8).
* **Descripción del Problema:**  
  El algoritmo criptográfico de derivación de claves BCrypt tiene una restricción matemática y de implementación estricta en su núcleo (bloque Eksblowfish): únicamente procesa los primeros 72 bytes de la clave de entrada. Cualquier carácter posterior es descartado silenciosamente sin generar error. Si un usuario o administrador ingresa una contraseña de 80 caracteres cuyas primeras 72 posiciones coinciden con las de otro intento, BCrypt consideraría ambas válidas, induciendo una debilidad de autenticación y una falsa sensación de seguridad.
* **Severidad:** Media (Riesgo de truncamiento inadvertido de entropía en contraseñas extensas o con caracteres multibyte).
* **Evidencia Visual:** `U-03_postman.png`
* **Causa Probable en el Código:**  
  La validación estándar `@Size(max = 72)` cuenta únicamente caracteres (code points de Java), pero en codificación UTF-8 un carácter especial, tilde o letra no latina puede ocupar de 2 a 4 bytes, superando la capacidad real de BCrypt a pesar de tener menos de 72 caracteres.
* **Corrección Propuesta / Aplicada:**  
  Se integró una validación en la capa de servicio (`UsuarioServiceImpl.java` y `AuthServiceImpl.java`) evaluando los bytes reales en UTF-8 previo al hasheo:
  ```java
  if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ReglaNegocioException("La contraseña no puede superar 72 bytes");
  }
  ```

---

### C3. Preguntas de Análisis Técnico

#### 1. ¿Por qué el login responde el mismo mensaje para un usuario inexistente, una contraseña incorrecta y una cuenta deshabilitada? ¿Qué información le daría a un atacante un mensaje distinto?
Responder con mensajes diferenciados (tales como *"El usuario no existe"* o *"Contraseña incorrecta"*) introduce una grave vulnerabilidad de **Enumeración de Usuarios (User Enumeration Attack)** catalogada en las directrices de seguridad de OWASP (*Authentication Cheat Sheet*). Si el backend revelara cuándo un usuario existe y cuándo no, un atacante automatizado podría recopilar listas masivas de correos o nombres de usuario válidos dentro de la organización mediante ataques de fuerza bruta o diccionarios. Posteriormente, al tener certeza de qué cuentas existen, el atacante concentraría sus recursos exclusivamente en descifrar contraseñas mediante técnicas de *Password Spraying* o *Credential Stuffing*. Asimismo, revelar que una cuenta se encuentra *"deshabilitada"* informa al adversario sobre la estructura operativa interna del personal de la botica. Unificar la respuesta bajo el código genérico `401 Unauthorized` con el mensaje `"Usuario o contraseña incorrectos"` bloquea toda filtración de metadatos del directorio de usuarios.

#### 2. Después de cambiar la contraseña (B1), el token anterior sigue siendo válido hasta que vence. ¿Por qué ocurre en una API sin estado? Propón una forma de invalidarlo.
Este comportamiento es inherente a la arquitectura **Stateless (sin estado)** del estándar JSON Web Token (RFC 7519). A diferencia de las sesiones clásicas gestionadas en memoria por el servidor mediante `JSESSIONID`, los tokens JWT son artefactos autónomos y autocontenidos: el servidor valida su autenticidad exclusivamente mediante la verificación matemática de la firma criptográfica (`verifyWith(claveSecreta)`) y la comparación temporal del claim `exp`. Como el backend no consulta un almacén central de tokens en cada petición, no se entera de manera nativa de que la credencial subyacente cambió mientras el token conserve una firma válida y no haya expirado. Para invalidarlo inmediatamente, se puede implementar:
1. **Token Blacklist en Memoria Distribuida (Redis):** Al cambiar la clave, el hash o ID del token anterior se almacena en una lista de revocación con un tiempo de vida (TTL) equivalente al tiempo restante hasta su expiración. El filtro `JwtAuthenticationFilter` comprueba la lista antes de autorizar la petición.
2. **Control por Versión de Credencial (`tokenVersion` o `passwordChangedAt`):** Añadir a la tabla `usuarios` una columna `fecha_modificacion_password`. En el JWT se incluye el timestamp de emisión (`iat`). En el filtro de seguridad, si `iat < fecha_modificacion_password`, se invalida la petición rechazándola con código 401.

#### 3. Si el secreto del JWT se publicara en GitHub, ¿qué podría hacer un atacante? ¿Qué harías de inmediato?
Si la clave simétrica (`app.jwt.secret`) quedase expuesta en un repositorio público, la seguridad de todo el sistema se consideraría totalmente comprometida. Conociendo el secreto y el algoritmo de firma (HS384), un atacante no necesita contraseñas ni acceso a la base de datos: puede utilizar bibliotecas como JJWT, OpenSSL o herramientas web para **firmar y generar tokens JWT arbitrarios a voluntad**, fabricando identidades falsas o adjudicándose a sí mismo el claim `"authorities": ["ROLE_ADMIN", "USUARIOS_ADMINISTRAR", "REPORTES_VER"]`. Con ello obtendría acceso irrestricto a los endpoints protegidos.  
**Plan de Acción Inmediato:**
1. **Revocación y Rotación Criptográfica:** Generar inmediatamente una nueva clave secreta robusta de alta entropía (mínimo 384 bits codificada en Base64) y desplegarla exclusivamente a través de variables de entorno del servidor o un gestor de secretos (Spring Cloud Vault, AWS Secrets Manager), nunca en código versionado.
2. **Invalidación Global de Sesiones:** El cambio del secreto en el servidor produce de forma automática la invalidez criptográfica de absolutamente todos los tokens previos que estaban en circulación.
3. **Auditoría de Logs:** Revisar minuciosamente los registros del servidor web e historial de peticiones para detectar si el secreto expuesto fue empleado para exfiltrar datos de clientes, productos o alterar registros contables.
4. **Depuración del Repositorio:** Reescribir el historial de Git (usando `git-filter-repo` o BFG Repo-Cleaner) para erradicar el secreto de los commits anteriores.

#### 4. ¿Qué ventaja tiene proteger los reportes con `@PreAuthorize` en el controlador frente a una regla por URL en `SecurityConfig`? ¿Y qué desventaja?
* **Ventajas de `@PreAuthorize`:**
  1. *Co-localización y Legibilidad:* La regla de seguridad se ubica directamente sobre el método que atiende la operación en el controlador (o en la capa de servicio), lo cual facilita el mantenimiento y la comprensión inmediata de las restricciones sin necesidad de navegar a archivos de configuración globales.
  2. *Expresividad y Reglas Dinámicas:* Permite el uso completo de Spring Expression Language (SpEL), habilitando validaciones complejas en tiempo de ejecución dependientes de los argumentos del método (por ejemplo, `@PreAuthorize("#username == authentication.name")` o validación de propiedad de recursos).
* **Desventajas de `@PreAuthorize`:**
  1. *Dispersión de la Política de Seguridad:* Las reglas de autorización quedan esparcidas a lo largo de múltiples controladores y clases, lo cual incrementa notablemente el riesgo de que un desarrollador olvide anotar un nuevo endpoint, dejándolo desprotegido ante clientes externos.
  2. *Dificultad de Auditoría Global:* En auditorías de seguridad perimetral es más complejo verificar la postura de defensa del sistema al no contar con un único punto centralizado de inspección como lo ofrece el encadenamiento `authorizeHttpRequests` en `SecurityConfig`.

---

## Parte D: Versionamiento y Verificación con Lista de Cotejo (2 Puntos)

### 1. Commits Registrados en la Rama `feature/seguridad-pruebas-vilca`
1. `test(seguridad): matriz de pruebas de autenticación y permisos`  
   *Registro de la matriz consolidada de 28 casos en `docs/matriz-seguridad-s10.md` y colección Postman.*
2. `feat(auth): cambio de contraseña del usuario autenticado`  
   *Implementación del DTO `CambioPasswordRequestDTO`, servicio y endpoint `PUT /api/v1/auth/password`.*
3. `feat(usuarios): asignar roles y activar o desactivar usuarios`  
   *Implementación de endpoints `PUT /api/v1/usuarios/{id}/roles` y `PATCH /api/v1/usuarios/{id}/estado`, protecciones de superusuario y validaciones.*

### 2. Autoevaluación frente a la Lista de Cotejo Oficial (20 / 20 Puntos)

| N.º | Criterio Observable | Puntaje | Cumplimiento | Evidencia en el Proyecto |
|:---:|---|:---:|:---:|---|
| **1** | La matriz tiene al menos 20 casos, con los 16 base y 4 propios de grupos distintos. | 2.0 | **SÍ** | Matriz en `docs/matriz-seguridad-s10.md` con 28 casos numerados (16 base + 4 propios + B-01 a B-08). |
| **2** | Cada caso indica código HTTP y mensaje esperados y obtenidos, y su estado. | 2.0 | **SÍ** | Columnas exhaustivas de código HTTP y cuerpo de mensaje contrastado en cada fila. |
| **3** | Los casos de token (alterado, de otra clave, vencido, sin Bearer) están ejecutados con evidencia. | 2.0 | **SÍ** | Casos T-01, T-02, T-03 y T-04 contemplados en la matriz y en la colección de Postman. |
| **4** | Los casos de permisos cubren los tres roles y distinguen 401 de 403. | 1.0 | **SÍ** | Casos P-01 a P-05 evaluando roles `ADMIN`, `VENDEDOR` y `ALMACENERO`, diferenciando autenticación de autorización. |
| **5** | Cada caso tiene su evidencia nombrada con el ID y la colección no guarda tokens reales. | 1.0 | **SÍ** | Nomenclatura estricta `<ID>_postman.png`; colección Postman con variables limpias `""`. |
| **6** | El cambio de contraseña toma el usuario del token, valida la actual con BCrypt y guarda el nuevo hash. | 1.5 | **SÍ** | Implementado en `AuthServiceImpl.cambiarPassword` usando `Authentication.getName()` y `passwordEncoder`. |
| **7** | El cambio de contraseña responde 204, 400 y 409 según el caso, y no registra contraseñas en el log. | 1.5 | **SÍ** | Códigos de respuesta integrados en `GlobalExceptionHandler` y registro seguro de logs. |
| **8** | La asignación de roles reemplaza las filas de `usuario_roles`, responde 400, 404 y 409 según el caso y el nuevo permiso aplica en la siguiente petición. | 2.0 | **SÍ** | Implementado en `UsuarioServiceImpl.asignarRoles`, impidiendo que el ADMIN se retire su propio rol. |
| **9** | La desactivación es solo para ADMIN, responde 404 y 409 según el caso y el token del desactivado deja de servir. | 2.0 | **SÍ** | Implementado en `UsuarioServiceImpl.cambiarEstado` y validado por `JwtAuthenticationFilter` en cada request. |
| **10** | Las fichas de hallazgos indican causa probable y corrección propuesta. | 1.5 | **SÍ** | Ficha H-01 detallando el límite de 72 bytes de BCrypt en UTF-8 y su solución en código. |
| **11** | Las cuatro preguntas de análisis se responden con argumentos y referencias al código. | 1.5 | **SÍ** | Sección C3 con respuestas técnicas fundamentadas en estándares OWASP y RFC 7519. |
| **12** | La rama tiene al menos tres commits en días distintos, los documentos en docs/ y la solicitud de incorporación. | 2.0 | **SÍ** | 3 commits atómicos en `feature/seguridad-pruebas-vilca`, archivos en `docs/` y Pull Request listo. |
| **TOTAL** | **Calificación Obtenida en Lista de Cotejo** | **20.0 / 20.0** | **100 %** | **Todos los criterios cubiertos de manera sobresaliente.** |

---
*Fin del Informe Técnico de Seguridad.*
