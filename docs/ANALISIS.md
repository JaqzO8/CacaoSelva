# Análisis breve y mejoras

| Situación anterior | Mejora implementada | Comprobación |
|---|---|---|
| Tres registros fijos en memoria | PostgreSQL, pool JDBC y migraciones con 30 registros ficticios | Lectura tras reiniciar API, sin duplicación de datos |
| Solo consultas | Casos de uso y endpoints de crear, actualizar, eliminar y contar pendientes | Pruebas de validación, CRUD HTTP y SQL real |
| UI con una única tabla y botón | Formulario, selección para editar, filtros, progreso y confirmación de eliminación | Prueba automatizada de controles JavaFX |
| Mensaje genérico tras toda falla | Validaciones visibles, sin datos obsoletos y advertencia ante escrituras no confirmadas | No se reintenta una escritura automáticamente |
| Monitor con intervalo fijo | Intervalo configurable, cambios de conteo, fallos consecutivos y recuperación explícita | API detenida/reiniciada mientras Monitor sigue activo |
| Pruebas en comandos separados | Script único y workflow de CI con PostgreSQL aislado | Verificación completa y reportes automáticos |

La persistencia se añadió sin mover reglas de negocio a controladores ni añadir dependencias de frameworks en el núcleo. `LoteWritePort` evita exigir operaciones de escritura a los consumidores de solo lectura. Las validaciones de peso, socio y estado se comparten entre creación y actualización.

En Desktop, conservar filas tras un error podía hacer que parecieran actuales. Se limpian durante las operaciones y se informa la última actualización. Los controles se bloquean mientras existe una operación en curso. Ante un timeout de escritura no se puede asumir que el servidor no guardó: se solicita consultar antes de repetir, y no se realizan reintentos automáticos.

En Monitor, una respuesta fallida no se interpreta como cero pendientes. Se preserva el último conteo solo para comparar un éxito posterior; los fallos se registran como fallos. Tras recuperar la conexión se registra la recuperación y, si corresponde, el cambio de conteo. El planificador no solapa consultas y se cierra de forma explícita.

Pendiente para una futura versión: paginación cuando aumente el volumen, control de edición concurrente mediante versión y políticas de acceso si deja de ser una demostración local. No hay métodos esenciales pendientes de implementación en el alcance CRUD solicitado.
