# Manual web de CacaoSelva

La web funciona en navegadores actuales de computadora, tableta y móvil, sin instalar Java. Abre la URL indicada en el README. Cuando el alojamiento gratuito está inactivo, la primera apertura puede tardar alrededor de un minuto.

## Crear cuenta e ingresar

1. Pulsa **Crear mi cuenta**.
2. Elige un usuario de 3 a 50 caracteres: letras sin acentos, números, puntos, guiones o guion bajo.
3. Introduce una contraseña de al menos 12 caracteres, hasta 72 bytes en UTF-8. No reutilices una contraseña de otro servicio.
4. Pulsa **Crear cuenta**. La aplicación inicia sesión y muestra los lotes compartidos.

Las cuentas públicas tienen rol **OPERADOR**: pueden consultar, crear y editar lotes. El administrador puede además registrar socios, eliminar lotes y crear usuarios con otros roles mediante la API. **CONSULTOR** tiene acceso de lectura. Los operadores reciben únicamente el ID y nombre del socio; el DNI y teléfono se reservan al administrador.

La sesión se conserva al recargar la misma pestaña y vence después de una hora. Pulsa **Cerrar sesión** al terminar en un dispositivo compartido. La contraseña no se guarda en el navegador.

## Consultar y gestionar

- Busca por nombre del socio o por su ID numérico; selecciona estado y cantidad por página. La búsqueda se actualiza al escribir.
- Usa **Anterior** y **Siguiente** para recorrer páginas. **Actualizar** consulta los últimos cambios realizados por otros usuarios.
- **Nuevo lote** abre el formulario. Selecciona un socio, un peso positivo con hasta tres decimales y un estado. **Guardar lote** confirma su almacenamiento.
- **Editar** permite cambiar los datos. Si otra persona modificó el lote entretanto, el servidor rechaza la versión anterior: cierra el formulario, actualiza y vuelve a editar.
- El administrador puede pulsar **Registrar socio**. Los DNI duplicados se rechazan con un mensaje claro.
- El administrador puede **Eliminar** un lote tras confirmar en el diálogo. Su historial de auditoría permanece en la base; el registro se elimina definitivamente de la lista.

Los registros son comunes a todos los usuarios autenticados. Evita escribir datos sensibles en los nombres. Los indicadores muestran los lotes que coinciden con los filtros, los pendientes globales y el peso de la página actual.

## En móvil

Los botones, filtros y formularios se adaptan al ancho disponible. En móvil, cada fila se presenta como una tarjeta con socio, peso, estado y acciones visibles; la página no necesita desplazamiento lateral. Los botones tienen tamaño táctil y los campos usan letra de 16 píxeles para evitar el zoom automático al escribir.

## Problemas habituales

- **Usuario ya registrado:** elige otro usuario o ingresa con tu cuenta existente.
- **Demasiados intentos:** respeta el tiempo de espera. El registro admite cinco intentos por IP por hora; el acceso, treinta por minuto. Los límites se comparten entre dispositivos que usan la misma IP pública.
- **No se pudo conectar:** espera el inicio del servidor gratuito y pulsa Actualizar.
- **No se pudo confirmar el resultado:** consulta primero la lista para comprobar si el cambio se guardó antes de repetirlo.
- **Sesión vencida:** vuelve a ingresar.

## Instalación y clonación

```powershell
git clone --branch deploy https://github.com/JaqzO8/CacaoSelva.git
cd CacaoSelva
pwsh -NoProfile -File scripts/database.ps1 start
.\mvnw.cmd clean install
pwsh -NoProfile -File scripts/run-api.ps1
```

Requiere Git, JDK 21, PostgreSQL 17 y PowerShell 7. Abre `http://localhost:5080`. La instancia local es independiente de la pública. El administrador local es `admin`; su contraseña aleatoria está en `.env.local` bajo `CACAOSELVA_ADMIN_PASSWORD`. Para Desktop y Monitor consulta [MANUAL_USUARIO.md](MANUAL_USUARIO.md).

## Pruebas automatizadas

```powershell
cd web-tests
npm ci
npx playwright install chromium
cd ..
pwsh -NoProfile -File scripts/verify.ps1 -Gui -Postman -Web
```

La validación crea y elimina una base temporal aislada. Comprueba Java, PostgreSQL, permisos, edición concurrente, JavaFX, cuatro tamaños de navegador, Postman y persistencia al reiniciar. Node.js 22 o posterior es necesario para las pruebas web.
