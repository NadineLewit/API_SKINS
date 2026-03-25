package skinsmarket.demo.service;

import skinsmarket.demo.dto.LoginRequest;
import skinsmarket.demo.dto.LoginResponse;
import skinsmarket.demo.dto.RegistroRequest;
import skinsmarket.demo.entity.Usuario;

public interface IUsuarioService {
    Usuario registrar(RegistroRequest request);
    LoginResponse login(LoginRequest request);
    Usuario obtenerPorUsername(String username);
    Usuario actualizar(String username, RegistroRequest request);
    void eliminar(String username);
}
