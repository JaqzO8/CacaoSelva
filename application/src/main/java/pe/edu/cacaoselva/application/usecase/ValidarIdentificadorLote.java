package pe.edu.cacaoselva.application.usecase;

import pe.edu.cacaoselva.application.exception.IdentificadorLoteInvalidoException;

final class ValidarIdentificadorLote {
    private ValidarIdentificadorLote() { }

    static void validar(Integer id) {
        if (id == null || id <= 0) {
            throw new IdentificadorLoteInvalidoException();
        }
    }
}
