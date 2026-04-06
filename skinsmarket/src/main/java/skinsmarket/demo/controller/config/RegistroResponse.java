package skinsmarket.demo.controller.config;

import skinsmarket.demo.controller.auth.AuthenticationResponse;

/**
 * RegistroResponse es ahora un alias de AuthenticationResponse.
 * Ambos endpoints (login y registro) devuelven solo el access_token.
 * El frontend decodifica el JWT para obtener email, rol y demás datos.
 */
public class RegistroResponse extends AuthenticationResponse {

    public RegistroResponse() { super(); }

    public RegistroResponse(String accessToken) {
        super(accessToken);
    }
}
