# Integración de MAURICIO en deploy

La rama `deploy` parte del commit `e9afbaac2cebb3d3d847e6ad439531f538316f94` de `MAURICIO`. Conserva sus cambios de catálogo de socios, autenticación JWT, roles, DTO, paginación, edición concurrente, auditoría y salud de API. La [revisión previa](REVISION_MAURICIO_2026-09-26.md) recoge el estado antes de integrar.

## Correcciones posteriores a la revisión

1. Desktop obtiene un catálogo de socios accesible al operador con ID y nombre. La ruta completa con DNI y teléfono continúa reservada al administrador.
2. Los botones de paginación observan propiedades numéricas de página y total; recalculan su estado al navegar. Los ID de búsqueda inválidos muestran un mensaje.
3. El controlador conserva el último filtro solicitado mientras una consulta está en curso; descarta el resultado anterior para aplicar el filtro pendiente.
4. Un socio inexistente devuelve 404. Un DNI o usuario duplicado devuelve 409, en lugar de un falso error de disponibilidad 503.

## Web y publicación

- Web en español, mismo origen que la API, con registro, login, logout, catálogo, filtros, paginación, indicadores, creación y edición de lotes. Administradores pueden registrar socios y eliminar lotes.
- Registro público con rol fijo OPERADOR, sin posibilidad de elegir ADMIN desde el cliente. Contraseñas BCrypt, JWT de una hora, límites por IP y cabeceras de seguridad.
- Vista móvil en tarjetas con acciones visibles, controles táctiles y formularios adaptables. No se usa HTML interpolado para mostrar nombres suministrados por usuarios.
- Los datos de negocio residen en PostgreSQL externo de Neon; Render aloja web y API. El perfil `prod` exige credenciales de entorno y autenticación.
- Docker multietapa con Java 21 y usuario sin privilegios. El contenedor de compilación instala unzip para que Maven Wrapper valide el mismo archivo ZIP y checksum configurados.
- GitHub Actions ejecuta pruebas y construye Docker antes de enviar a Render el SHA probado; comprueba salud y SHA publicado. Auto-Deploy de Render queda desactivado.
- Manual web, clonación, publicación, diagrama de arquitectura y respaldo/restauración documentados. Ninguna credencial de producción se incluye en Git.

## Validación

La validación local completa del 30 de septiembre de 2026 pasó con **123 pruebas Java**, **4 pruebas web** en anchos de 1365, 768, 375 y 320 píxeles y **23 comprobaciones Postman** en 13 solicitudes. Incluye PostgreSQL real en una base temporal independiente, permisos, conflictos de edición, JavaFX real y persistencia al reiniciar la API. El monitor detecta la caída, se recupera y observa el conteo cambiar de 21 a 20.

Las pruebas web comprueban registro, creación, edición, lectura desde otro cliente, recarga de sesión, filtros, paginación, borrado por administrador, ausencia de errores JavaScript y ausencia de desbordamiento lateral en página y formularios. Capturas y reportes se generan como artefactos de Actions. Consulta el resultado del workflow del commit que se vaya a publicar; los resultados locales no sustituyen la validación de la imagen remota.

## Límites de persistencia

La base sobrevive a reinicios y despliegues de la aplicación. La conservación depende de mantener el proyecto de Neon y sus credenciales, respetar sus cuotas y disponer de respaldos externos. El plan gratuito no garantiza acceso permanente ni protección ante eliminación de la base. Consulta [DESPLIEGUE.md](DESPLIEGUE.md).
