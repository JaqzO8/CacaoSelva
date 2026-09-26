package pe.edu.cacaoselva.api.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.cacaoselva.api.dto.GuardarSocioRequest;
import pe.edu.cacaoselva.api.dto.SocioResponse;
import pe.edu.cacaoselva.api.mapper.LoteResponseMapper;
import pe.edu.cacaoselva.application.usecase.CrearSocioUseCase;
import pe.edu.cacaoselva.application.usecase.ListarSociosUseCase;
import pe.edu.cacaoselva.application.usecase.BuscarSocioPorDniUseCase;

/** Controlador REST para socios (Fase 1). */
@RestController
@RequestMapping("/socios")
public class SocioController {
    private final CrearSocioUseCase crearSocio;
    private final ListarSociosUseCase listarSocios;
    private final BuscarSocioPorDniUseCase buscarPorDni;

    public SocioController(CrearSocioUseCase crearSocio, ListarSociosUseCase listarSocios,
                           BuscarSocioPorDniUseCase buscarPorDni) {
        this.crearSocio = crearSocio;
        this.listarSocios = listarSocios;
        this.buscarPorDni = buscarPorDni;
    }

    @GetMapping
    public List<SocioResponse> listar() {
        return listarSocios.execute().stream().map(LoteResponseMapper::toSocioResponse).toList();
    }

    @GetMapping("/buscar")
    public SocioResponse buscarPorDni(@RequestParam String dni) {
        return LoteResponseMapper.toSocioResponse(buscarPorDni.execute(dni));
    }

    @PostMapping
    public ResponseEntity<SocioResponse> crear(@RequestBody GuardarSocioRequest request) {
        SocioResponse response = LoteResponseMapper.toSocioResponse(crearSocio.execute(request.toCommand()));
        return ResponseEntity.created(URI.create("/socios/" + response.id())).body(response);
    }
}
