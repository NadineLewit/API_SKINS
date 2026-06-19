package skinsmarket.demo.controller.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SteamAuthResult {
    private AuthenticationResponse authentication;
    private String steamId64;
}
