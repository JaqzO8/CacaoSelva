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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cacaoselva.api.dto.LoteResponse;
import pe.edu.cacaoselva.api.dto.PaginaLotesResponse;
import pe.edu.cacaoselva.api.dto.GuardarLoteRequest;
import pe.edu.cacaoselva.api.dto.ConteoPendientesResponse;
import pe.edu.cacaoselva.api.mapper.LoteResponseMapper;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.usecase.BuscarLotePorIdUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesPaginadoUseCase;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.application.port.AuditoriaQueryPort;
import pe.edu.cacaoselva.application.dto.EventoAuditoria;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;
import pe.edu.cacaoselva.application.exception.IdentificadorLoteInvalidoException;

@RestController
@RequestMapping("/lotes")
public class LoteController {
    private final ListarLotesUseCase listarLotes;
    private final ListarLotesPaginadoUseCase listarLotesPaginado;
    private final BuscarLotePorIdUseCase buscarLote;
    private final CrearLoteUseCase crearLote;
    private final ActualizarLoteUseCase actualizarLote;
    private final EliminarLoteUseCase eliminarLote;
    private final ContarLotesPendientesUseCase contarPendientes;
    private final AuditoriaQueryPort auditoria;

    public LoteController(ListarLotesUseCase listarLotes, ListarLotesPaginadoUseCase listarLotesPaginado,
                          BuscarLotePorIdUseCase buscarLote,
                          CrearLoteUseCase crearLote, ActualizarLoteUseCase actualizarLote,
                          EliminarLoteUseCase eliminarLote, ContarLotesPendientesUseCase contarPendientes,
                          AuditoriaQueryPort auditoria) {
        this.listarLotes = listarLotes;
        this.listarLotesPaginado = listarLotesPaginado;
        this.buscarLote = buscarLote;
        this.crearLote = crearLote;
        this.actualizarLote = actualizarLote;
        this.eliminarLote = eliminarLote;
        this.contarPendientes = contarPendientes;
        this.auditoria = auditoria;
    }

    @GetMapping
    public List<LoteResponse> listar() {
        return listarLotes.execute().stream().map(LoteResponseMapper::toResponse).toList();
    }

    @GetMapping(params = "page")
    public PaginaLotesResponse listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EstadoLote estado,
            @RequestParam(required = false) Integer socioId,
            @RequestParam(required = false) String socio) {
        var filtro = new FiltroLotes(estado, socioId, socio, page, size);
        var pagina = listarLotesPaginado.execute(filtro);
        return new PaginaLotesResponse(
                pagina.content().stream().map(LoteResponseMapper::toResponse).toList(),
                pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
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
    @Transactional
    public ResponseEntity<LoteResponse> crear(@RequestBody GuardarLoteRequest request,
            @org.springframework.web.bind.annotation.RequestAttribute(value = "usuario", required = false) String usuario) {
        LoteResponse response = LoteResponseMapper.toResponse(crearLote.execute(request.toCommand(), usuario));
        return ResponseEntity.created(URI.create("/lotes/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public LoteResponse actualizar(@PathVariable Integer id, @RequestBody GuardarLoteRequest request,
            @org.springframework.web.bind.annotation.RequestAttribute(value = "usuario", required = false) String usuario) {
        if (request.version() == null) throw new DatosLoteInvalidosException("La versión del lote es obligatoria para actualizarlo.");
        return LoteResponseMapper.toResponse(actualizarLote.execute(id, request.toCommand(), usuario));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestAttribute(value = "usuario", required = false) String usuario) {
        eliminarLote.execute(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    public List<EventoAuditoria> historial(@PathVariable Integer id) {
        if (id == null || id <= 0) throw new IdentificadorLoteInvalidoException();
        return auditoria.listarPorLote(id);
    }
}
