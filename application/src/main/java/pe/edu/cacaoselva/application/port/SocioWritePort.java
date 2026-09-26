package pe.edu.cacaoselva.application.port;

import pe.edu.cacaoselva.domain.model.DatosSocio;
import pe.edu.cacaoselva.domain.model.Socio;

/** Puerto de escritura para socios (ISP). */
public interface SocioWritePort {
    Socio create(DatosSocio datos);
}
