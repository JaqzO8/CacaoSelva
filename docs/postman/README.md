# CacaoSelva - Pruebas API

Importa `CacaoSelva.postman_collection.json` en Postman. La colección contiene 12 peticiones y 22 aserciones para consultas, CRUD y validación de errores. No requiere autenticación.

## Ejecución automática recomendada

Desde la raíz, con JDK 21, PostgreSQL y Node.js/npm:

```powershell
pwsh -NoProfile -File scripts/verify.ps1 -Postman
```

El script crea una base temporal, ejecuta Maven, inicia la API de prueba en el puerto 5081, ejecuta Newman, comprueba reinicio y recuperación, y limpia su base de prueba. No modifica la base normal.

## Ejecutar desde Postman

1. Inicia la API desde la raíz del proyecto.
2. Importa el archivo de esta carpeta.
3. Configura `baseUrl = http://localhost:5080` en variables de colección.
4. Ejecuta la colección completa y en orden; P06 guarda automáticamente el ID que utilizarán P07–P10.

La colección espera una base con la carga inicial: 30 lotes y 20 pendientes. Si modificaste los datos normales, usa la automatización con una base aislada para obtener resultados repetibles. Las peticiones CRUD crean y eliminan su propio registro de prueba.

| Caso | Método y ruta | Resultado |
|---|---|---|
| P01 | GET /lotes | 200, 30 lotes y 20 pendientes |
| P02 | GET /lotes/1 | 200, Ana, 120.5 kg |
| P03 | GET /lotes/999 | 404, error consistente |
| P04A | GET /lotes/abc | 400, error consistente |
| P04B | GET /lotes/0 | 400, error consistente |
| P05 | GET /lotes/pendientes/conteo | 200, pendientes = 20 |
| P06 | POST /lotes | 201, ID generado y Location |
| P07 | PUT /lotes/{{loteId}} | 200, actualización |
| P08 | GET /lotes/{{loteId}} | 200, valores modificados |
| P09 | DELETE /lotes/{{loteId}} | 204 sin cuerpo |
| P10 | GET /lotes/{{loteId}} | 404 tras eliminar |
| P11 | POST /lotes con peso negativo | 400 |

## Crear manualmente los scripts principales

Crea una colección llamada **CacaoSelva - Pruebas API**, agrega `baseUrl` y las peticiones anteriores. Coloca cada script en Post-response / Tests.

### Listado

```javascript
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("30 lotes y 20 pendientes", () => {
    const data = pm.response.json();
    pm.expect(data).to.be.an("array");
    pm.expect(data.length).to.eql(30);
    pm.expect(data.filter(lote => lote.estado === "PENDIENTE").length).to.eql(20);
});
```

### Creación

```json
{"socio":"Prueba Postman","pesoKg":12.375,"estado":"PENDIENTE"}
```

```javascript
pm.test("HTTP 201", () => pm.response.to.have.status(201));
pm.test("Identidad generada", () => {
    const lote = pm.response.json();
    pm.expect(lote.id).to.be.above(30);
    pm.collectionVariables.set("loteId", lote.id);
    pm.expect(pm.response.headers.get("Location")).to.eql("/lotes/" + lote.id);
});
```

### Error

Para un inexistente espera 404; para ID o datos inválidos espera 400:

```javascript
pm.test("HTTP 404", () => pm.response.to.have.status(404));
pm.test("Error consistente", () => {
    const error = pm.response.json();
    pm.expect(error.status).to.eql(pm.response.code);
    pm.expect(error.message).to.be.a("string").and.not.empty;
    pm.expect(error.timestamp).to.be.a("string");
});
```

## Recuperación

La prueba automatizada crea un registro, detiene la API, verifica que el Monitor continúe activo, reinicia la API y lee el mismo registro persistido. Comprueba que la carga inicial no se duplicó. Finalmente elimina su registro y espera a que Monitor observe el nuevo conteo. En Postman, con la API detenida se verá un error de conexión, sin código HTTP.

Para ejecutar únicamente la colección con la API ya activa:

```powershell
npx --yes newman@6.2.1 run docs/postman/CacaoSelva.postman_collection.json
```

Los scripts de la colección están probados con Newman; no se automatizó la interfaz gráfica de Postman.
