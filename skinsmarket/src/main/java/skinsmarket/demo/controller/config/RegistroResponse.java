package skinsmarket.demo.controller.config;

import skinsmarket.demo.controller.auth.AuthenticationResponse;

/**
 * RegistroResponse es ahora un alias de AuthenticationResponse.
 * Ambos endpoints (login y registro) devuelven solo el access_token.
 * Para datos del perfil, el frontend debe consultar /api/v1/users/me.
 */
public class RegistroResponse extends AuthenticationResponse {

    public RegistroResponse() { super(); }

    public RegistroResponse(String accessToken) {
        super(accessToken);
    }
}
