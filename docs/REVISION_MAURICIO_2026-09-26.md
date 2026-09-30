# Revisión de MAURICIO antes de integrar en deploy

Fecha: 26 de septiembre de 2026, hora de Lima.

**Conclusión:** los cambios están publicados y la automatización pasa, pero conviene corregir los cuatro problemas funcionales reproducidos antes de integrarlos en `deploy`. Esta revisión no modifica `main` ni `MAURICIO`, ni crea o integra `deploy`.

## Versiones revisadas

- Base `main`: `8d65735eb859d0734d748264f60d10c148fa5c96`.
- Rama `MAURICIO`: [`e9afbaac2cebb3d3d847e6ad439531f538316f94`](https://github.com/JaqzO8/CacaoSelva/commit/e9afbaac2cebb3d3d847e6ad439531f538316f94).
- Un commit adicional, de `mausol75`: «Completar plan de mejoras de CacaoSelva».
- 104 archivos cambiados, 2.388 líneas añadidas y 281 eliminadas.
- `main` es antecesor de `MAURICIO`: con estas revisiones, la integración permite avance directo sin conflictos de Git. Esto no garantiza ausencia de problemas funcionales.
- [Comparación en GitHub](https://github.com/JaqzO8/CacaoSelva/compare/main...MAURICIO).

## Listado de lo implementado

| Área | Cambios comprobados en el código |
|---|---|
| Socios | Nuevos modelos, validación de DNI de ocho dígitos, nombre, zona y teléfono; repositorio JDBC; alta, listado y búsqueda por DNI mediante API. No se implementan edición y eliminación de socios. |
| Relación socio–lote | Los lotes pasan de guardar el nombre como texto a referenciar `socioId`. La migración convierte los nombres existentes en socios con DNI temporal y conserva los lotes. |
| Autenticación | Login mediante JWT firmado con caducidad de una hora, contraseñas con BCrypt, usuario administrador inicial con contraseña configurable y alta de usuarios por un administrador. |
| Autorización | Roles `ADMIN`, `OPERADOR` y `CONSULTOR`; filtro HTTP con respuestas 401/403. ADMIN tiene acceso completo; OPERADOR puede leer y crear/actualizar lotes; CONSULTOR puede consultar lotes y conteo. |
| Consultas | Paginación `page`/`size`, límite de 100 por página y filtros por estado, ID y nombre de socio. Se conserva `GET /lotes` sin paginación. |
| Edición simultánea | Campo `version`; PUT exige la versión leída, la incrementa y devuelve 409 ante una edición desactualizada. Este control se aplica a actualización, no a eliminación. |
| Auditoría | Tabla de eventos de creación, actualización y eliminación de lotes, usuario, fecha y datos JSON; consulta `/lotes/{id}/historial`. Las escrituras de lotes y su auditoría comparten transacción. |
| Desktop | Ventana de login, selección de socios, consultas paginadas, controles Anterior/Siguiente, filtros en el servidor, aviso de conflicto y comprobación de disponibilidad de la API cada 15 segundos. |
| Monitor | Autenticación por token o cuenta, renovación anticipada cuando dispone de usuario/contraseña, comprobación de salud y registro de cambios UP/DOWN, manteniendo conteo y recuperación. Un token proporcionado sin credenciales no se renueva. |
| Operación | Actuator `health`, `info` y `metrics`; seis migraciones nuevas V3–V8; generación local de secreto JWT y contraseña de admin; scripts `run-api.ps1` y `run-monitor.ps1`; ajuste del Maven Wrapper de Windows. V8 retira el administrador de demostración introducido por V5. |
| Documentación y pruebas | Actualizaciones parciales de README, manual y validación; Postman añade login y envía JWT y versiones. La suite pasa de 97 a 118 casos Java y de 22 a 23 aserciones Postman. |

## Problemas reproducidos

### 1. Alta prioridad: OPERADOR no puede completar el CRUD permitido desde Desktop

El filtro niega todas las rutas `/socios` a usuarios distintos de ADMIN. Desktop siempre intenta cargar esa lista y exige seleccionar uno de sus elementos para guardar un lote. Un operador puede crear por HTTP si conoce un `socioId`, pero el formulario no permite seleccionarlo: no puede crear ni editar normalmente desde Desktop.

**Reproducción:** crear un usuario OPERADOR, iniciar sesión y solicitar `GET /socios` devuelve **403**; con el mismo token, `POST /lotes` y un socio existente devuelve **201**. En el formulario, una lista vacía impide superar «Selecciona un socio de la lista».

**Corrección propuesta:** ofrecer a los roles que lo necesiten un catálogo autorizado con ID y nombre de socio, o ajustar los permisos de lectura y el cliente sin abrir innecesariamente la administración de socios.

Referencias: [restricción de permisos](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/api/src/main/java/pe/edu/cacaoselva/api/filter/JwtAuthFilter.java#L72), [carga de socios en Desktop](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/desktop/src/main/java/pe/edu/cacaoselva/desktop/CacaoSelvaDesktopApplication.java#L59), [selección obligatoria](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/desktop/src/main/java/pe/edu/cacaoselva/desktop/view/LotesWindow.java#L177).

### 2. Prioridad media: Anterior permanece deshabilitado al avanzar de página

`paginaAnteriorInvalida()` crea un binding sin dependencias observables. Conserva el resultado inicial aunque cambie `currentPage`, de modo que no se puede regresar desde la segunda página mediante el botón.

**Reproducción con controles JavaFX reales:** mostrar página 1 de 2, simular consulta y mostrar página 2 de 2. `anterior.isDisabled()` devuelve **true** en ambas; en la segunda debería ser false.

**Corrección propuesta:** usar una propiedad observable para la página actual o invalidar el binding al cambiarla.

Referencia: [binding del botón Anterior](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/desktop/src/main/java/pe/edu/cacaoselva/desktop/view/LotesWindow.java#L75).

### 3. Prioridad media: se pierde el último filtro escrito durante una consulta

Los filtros siguen editables durante la petición. El controlador cambia el campo `filtro`, pero `ejecutar()` descarta la nueva operación cuando `consultando` es true. Al terminar la petición anterior, no solicita el último filtro. La tabla puede mostrar resultados que no corresponden al texto visible.

**Reproducción:** solicitar «Ana», solicitar «Rosa» antes de que la UI reciba la respuesta anterior y entregar esa respuesta. La única consulta realizada es **[Ana]**; nunca se consulta Rosa.

**Corrección propuesta:** capturar el filtro de cada petición y procesar el último pendiente al terminar, o cancelar/sustituir consultas de lectura sin descartar silenciosamente el cambio. Añadir una prueba de escritura rápida con respuesta demorada.

Referencias: [actualización del filtro](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/desktop/src/main/java/pe/edu/cacaoselva/desktop/controller/LotesController.java#L54), [operación descartada](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/desktop/src/main/java/pe/edu/cacaoselva/desktop/controller/LotesController.java#L87).

### 4. Prioridad media: errores de datos se presentan como caída del servicio

Las violaciones de clave foránea y de unicidad se convierten en `PersistenciaLoteException`, que responde 503. Son datos inválidos o duplicados, no una indisponibilidad de PostgreSQL.

**Reproducción:** crear un lote con `socioId=2147483647` devuelve **503**; registrar un socio con un DNI ya existente también devuelve **503**. El servidor y PostgreSQL permanecen disponibles.

**Corrección propuesta:** traducir la ausencia de socio a un error de negocio 400/404 y el DNI duplicado a 409; reservar 503 para fallos de almacenamiento o conexión.

Referencias: [inserción de lotes](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/infrastructure/src/main/java/pe/edu/cacaoselva/infrastructure/repository/JdbcLoteRepository.java#L137), [inserción de socios](https://github.com/JaqzO8/CacaoSelva/blob/e9afbaac2cebb3d3d847e6ad439531f538316f94/infrastructure/src/main/java/pe/edu/cacaoselva/infrastructure/repository/JdbcSocioRepository.java#L95).

## Validación realizada

- GitHub Actions: [ejecución aprobada del commit revisado](https://github.com/JaqzO8/CacaoSelva/actions/runs/36257218071).
- Ejecución local independiente: `pwsh -NoProfile -File scripts/verify.ps1 -Gui -Postman`, en un worktree del commit exacto y una instancia PostgreSQL separada en el puerto 55434.
- **118 pruebas Java, cero fallos, errores u omitidas:** dominio 30, aplicación 24, infraestructura 27, API 21, Desktop 9 y Monitor 7.
- **13 peticiones y 23 aserciones Postman**, todas correctas.
- Persistencia después de reiniciar la API, recuperación del Monitor y limpieza de las bases temporales: correctas.
- Prueba adicional de actualización **V2 → V8**: se crearon los 30 lotes iniciales más un lote en el esquema antiguo; después de migrar se conservaron los 31, incluido el ID, nombre del socio con acento, peso decimal y estado del registro adicional. Su versión inicial quedó en 1.
- Comprobaciones específicas de revisión: controles JavaFX para navegación, controlador con filtro pendiente y HTTP real para permisos y validación. Reprodujeron los cuatro problemas anteriores, que no impiden que la suite existente termine en verde.

No se aplicaron estas migraciones a la base de uso habitual ni se modificó el código de la rama revisada.

## Consideraciones para integrar después de la revisión

1. Corregir los problemas reproducidos y añadir pruebas de regresión para esos escenarios.
2. Completar el manual y el diagrama: algunas secciones aún describen el socio como texto libre y la tabla antigua con columna `socio`; ahora el formulario usa un selector y la tabla guarda `socio_id`.
3. Actualizar API, Desktop y Monitor juntos. El contrato cambia de `socio` a `socioId`, exige JWT y requiere `version` en PUT; los clientes antiguos no son compatibles.
4. Respaldar la base antes de ejecutar las nuevas migraciones. V4 elimina la antigua columna `socio`; volver únicamente al binario anterior no revierte ese cambio de esquema. Los DNI generados para socios históricos son temporales y requieren revisión si se usan datos reales.
5. Después de revisar y corregir, crear `deploy` con el estado aprobado y comprobar GitHub Actions sobre el commit resultante. No se ha realizado esa integración en esta revisión.
