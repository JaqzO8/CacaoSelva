# Validación de la versión con PostgreSQL

Fecha: 21 de septiembre de 2026. Entorno: Windows, JDK 21.0.8, Maven Wrapper 3.9.16 y PostgreSQL 17.6.

## Pruebas Java

| Módulo | Pruebas ejecutadas | Resultado |
|---|---:|---|
| domain | 11 | Correctas |
| application | 24 | Correctas |
| infrastructure | 27 | Correctas |
| api (integración PostgreSQL) | 19 | Correctas |
| desktop (incluye controles JavaFX) | 9 | Correctas |
| monitor | 7 | Correctas |
| **Total** | **97** | **0 fallos, 0 errores, 0 omitidas** |

Se validaron los seis casos de uso; IDs y datos inválidos; precisión decimal; repositorio JDBC; HTTP real; errores y timeout; CRUD completo contra PostgreSQL; filtros, creación, edición y recuperación en controles JavaFX; prevención de escrituras duplicadas; y ciclo de vida del monitor.

La captura de [Desktop](images/desktop.png) corresponde a la prueba gráfica con tres registros de control. La base de uso normal contiene 30 registros.

## Colección Postman

Ejecutada con Newman 6.2.1 sobre un JAR real de la API y una base PostgreSQL temporal: **12 peticiones, 22 aserciones, cero fallos**. Incluye consultas, creación, actualización, lectura de cambios, eliminación y validaciones 400/404.

No se utilizó la interfaz gráfica de Postman; se ejecutaron los scripts de la colección importable.

## Persistencia y recuperación

La prueba de procesos independientes:

1. Inicia la API y verifica 30 registros iniciales.
2. Crea un registro pendiente con peso 44.125 kg.
3. Inicia el Monitor, que observa 21 pendientes.
4. Detiene la API y comprueba que Monitor siga activo registrando fallos.
5. Reinicia la API y lee el registro creado con su misma identidad y datos.
6. Verifica 31 registros: las migraciones no duplican los 30 iniciales.
7. Comprueba recuperación del Monitor, elimina el registro de prueba y observa el cambio 21 → 20.
8. Cierra los procesos de prueba y elimina exclusivamente la base temporal generada.

También se cargó la base normal `cacaoselva`, se detuvo la API, se detuvo y reinició la instancia PostgreSQL del proyecto y se consultó nuevamente mediante SQL: **30 lotes, 20 pendientes**. Los datos permanecieron en disco. Se verificó que el usuario `cacaoselva` no es superusuario.

## Automatización

```powershell
pwsh -NoProfile -File scripts/verify.ps1 -Gui -Postman
```

El script prepara una base distinta por ejecución. Maven ejecuta pruebas unitarias y Failsafe ejecuta las pruebas PostgreSQL con el perfil `integration`. El smoke test emplea el puerto 5081 y el Monitor usa un intervalo de un segundo solo durante esa prueba.

Reportes:

- `*/target/surefire-reports/`: pruebas unitarias y JavaFX.
- `api/target/failsafe-reports/`: integración PostgreSQL.
- `.tools/validation/cacaoselva_test_*/`: logs de API/Monitor y reporte JSON de Newman.
- `.tools/verify-postgres-final.log`: ejecución local de la automatización.

El workflow `.github/workflows/ci.yml` ejecuta el mismo procedimiento en GitHub Actions, con PostgreSQL local y Xvfb. El estado de cada revisión y sus reportes se consultan en [Pruebas CacaoSelva](https://github.com/JaqzO8/CacaoSelva/actions/workflows/ci.yml).

## Revisión de arquitectura

Dominio y aplicación siguen libres de Spring, JDBC, JavaFX y Jackson. El repositorio en memoria se conserva como referencia de solo lectura; la API usa JDBC. El puerto de escritura está separado del de consultas. Los controladores delegan en casos de uso. Las migraciones y el pool viven en la capa externa. No quedan TODOs ni métodos esenciales sin implementar dentro del alcance solicitado.
