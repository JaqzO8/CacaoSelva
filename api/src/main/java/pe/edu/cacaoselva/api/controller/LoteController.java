package pe.edu.cacaoselva.api.controller;

import java.util.List;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cacaoselva.api.dto.LoteResponse;
import pe.edu.cacaoselva.api.dto.GuardarLoteRequest;
import pe.edu.cacaoselva.api.dto.ConteoPendientesResponse;
import pe.edu.cacaoselva.api.mapper.LoteResponseMapper;
import pe.edu.cacaoselva.application.usecase.BuscarLotePorIdUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesUseCase;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;

@RestController
@RequestMapping("/lotes")
public class LoteController {
    private final ListarLotesUseCase listarLotes;
    private final BuscarLotePorIdUseCase buscarLote;
    private final CrearLoteUseCase crearLote;
    private final ActualizarLoteUseCase actualizarLote;
    private final EliminarLoteUseCase eliminarLote;
    private final ContarLotesPendientesUseCase contarPendientes;

    public LoteController(ListarLotesUseCase listarLotes, BuscarLotePorIdUseCase buscarLote,
                          CrearLoteUseCase crearLote, ActualizarLoteUseCase actualizarLote,
                          EliminarLoteUseCase eliminarLote, ContarLotesPendientesUseCase contarPendientes) {
        this.listarLotes = listarLotes;
        this.buscarLote = buscarLote;
        this.crearLote = crearLote;
        this.actualizarLote = actualizarLote;
        this.eliminarLote = eliminarLote;
        this.contarPendientes = contarPendientes;
    }

    @GetMapping
    public List<LoteResponse> listar() {
        return listarLotes.execute().stream().map(LoteResponseMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public LoteResponse buscar(@PathVariable Integer id) {
        return LoteResponseMapper.toResponse(buscarLote.execute(id));
    }

    @GetMapping("/pendientes/conteo")
    public ConteoPendientesResponse contarPendientes() {
        return new ConteoPendientesResponse(contarPendientes.execute());
    }

    @PostMapping
    public ResponseEntity<LoteResponse> crear(@RequestBody GuardarLoteRequest request) {
        LoteResponse response = LoteResponseMapper.toResponse(crearLote.execute(request.toCommand()));
        return ResponseEntity.created(URI.create("/lotes/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public LoteResponse actualizar(@PathVariable Integer id, @RequestBody GuardarLoteRequest request) {
        return LoteResponseMapper.toResponse(actualizarLote.execute(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        eliminarLote.execute(id);
        return ResponseEntity.noContent().build();
    }
}
