package pe.edu.cacaoselva.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.cacaoselva.api.dto.LoginRequest;
import pe.edu.cacaoselva.api.dto.LoginResponse;
import pe.edu.cacaoselva.application.usecase.AutenticarUsuarioUseCase;
import pe.edu.cacaoselva.domain.model.Credenciales;
import pe.edu.cacaoselva.api.dto.CrearUsuarioRequest;
import pe.edu.cacaoselva.application.usecase.RegistrarUsuarioUseCase;
import pe.edu.cacaoselva.application.dto.UsuarioDto;

/** Controlador REST para autenticación JWT (Fase 3). */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AutenticarUsuarioUseCase autenticar;
    private final RegistrarUsuarioUseCase registrar;
    private final boolean registroPublico;

    public AuthController(AutenticarUsuarioUseCase autenticar, RegistrarUsuarioUseCase registrar,
            @org.springframework.beans.factory.annotation.Value("${cacaoselva.registration.enabled:true}") boolean registroPublico) {
        this.autenticar = autenticar;
        this.registrar = registrar;
        this.registroPublico = registroPublico;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = autenticar.execute(new Credenciales(request.usuario(), request.contrasena()));
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioDto> registrarse(@RequestBody LoginRequest request) {
        if (!registroPublico) return ResponseEntity.status(403).build();
        // El cliente nunca elige su rol al registrarse públicamente.
        return ResponseEntity.status(201).body(registrar.execute(request.usuario(), request.contrasena(),
                pe.edu.cacaoselva.domain.model.Rol.OPERADOR));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDto> crearUsuario(@RequestBody CrearUsuarioRequest request) {
        UsuarioDto usuario = registrar.execute(request.usuario(), request.contrasena(), request.rol());
        return ResponseEntity.status(201).body(usuario);
    }
}
