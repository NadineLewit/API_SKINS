package skinsmarket.demo.service;

import java.util.regex.Pattern;

/**
 * Clase utilitaria con métodos estáticos de validación de datos de entrada.
 *
 * Idéntica al InfoValidator del TPO aprobado.
 * Centraliza las reglas de validación para evitar repetir lógica
 * en múltiples servicios (AuthenticationService, SkinServiceImpl, UserServiceImpl).
 *
 * Todos los métodos son estáticos para poder usarlos sin instanciar la clase.
 */
public class InfoValidator {

    /**
     * Valida que una contraseña cumpla los requisitos mínimos de seguridad
     * y que coincida con su confirmación.
     *
     * Requisitos:
     *   1. Las dos contraseñas deben ser iguales (password == passwordRepeat)
     *   2. Mínimo 5 caracteres de longitud
     *   3. Debe contener al menos una letra
     *   4. Debe contener al menos un número
     *
     * @param password       contraseña ingresada por el usuario
     * @param passwordRepeat confirmación de la contraseña
     * @return true si la contraseña es válida, false en caso contrario
     */
    public static boolean isValidPassword(String password, String passwordRepeat) {
        // 1. Verificar que ambas contraseñas sean iguales
        if (!password.equals(passwordRepeat)) {
            System.out.println("Error: Las contraseñas no coinciden.");
            return false;
        }

        // 2. Verificar largo mínimo de 5 caracteres
        if (password.length() < 5) {
            System.out.println("Error: La contraseña debe tener al menos 5 caracteres.");
            return false;
        }

        // 3. Verificar presencia de al menos una letra y un número
        boolean tieneLetra = false;
        boolean tieneNumero = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c))  tieneLetra  = true;
            if (Character.isDigit(c))   tieneNumero = true;
            // Salida anticipada cuando ya se encontraron ambos tipos
            if (tieneLetra && tieneNumero) break;
        }

        if (!tieneLetra) {
            System.out.println("Error: La contraseña debe contener al menos una letra.");
            return false;
        }
        if (!tieneNumero) {
            System.out.println("Error: La contraseña debe contener al menos un número.");
            return false;
        }

        return true;
    }

    /**
     * Valida que un email tenga un formato correcto.
     *
     * Usa una expresión regular que acepta los formatos más comunes:
     *   user@example.com, user.name@sub.domain.org, user+tag@mail.co
     *
     * @param email email a validar
     * @return true si el email tiene formato válido, false si es null o inválido
     */
    public static boolean isValidEmail(String email) {
        // Expresión regular para validar formato de email RFC-compatible
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(regex);
        // Retorna false si el email es null o no coincide con el patrón
        return email != null && pattern.matcher(email).matches();
    }

    /**
     * Valida que el stock de una skin sea un número no negativo.
     *
     * El stock puede ser 0 (agotado) pero no puede ser negativo.
     *
     * @param stock valor de stock a validar
     * @return true si stock >= 0, false si es negativo
     */
    public static boolean isValidStock(Integer stock) {
        return stock >= 0;
    }

    /**
     * Valida que el precio de una skin sea un número positivo.
     *
     * El precio debe ser estrictamente mayor a 0 (no se admiten skins gratuitas).
     *
     * @param price precio a validar
     * @return true si price > 0, false si es 0 o negativo
     */
    public static boolean isValidPrice(Double price) {
        return price > 0;
    }

    /**
     * Valida que el descuento esté en el rango permitido [0.0, 1.0].
     *
     * 0.0 = sin descuento, 1.0 = 100% de descuento (gratis).
     * Valores fuera de este rango no tienen sentido de negocio.
     *
     * @param discount valor de descuento a validar
     * @return true si 0.0 <= discount <= 1.0, false en caso contrario
     */
    public static boolean isValidDiscount(Double discount) {
        return discount >= 0 && discount <= 1;
    }
}
