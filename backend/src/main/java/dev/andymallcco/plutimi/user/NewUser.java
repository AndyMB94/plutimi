package dev.andymallcco.plutimi.user;

/**
 * Datos para dar de alta un usuario desde otro componente ({@code AdminInitializer} y, en la 1.7,
 * el registro de {@code auth}). No es un DTO de la API: quien lo construye ya validó los datos.
 *
 * @param rawPassword contraseña en claro; {@code UserService} la hashea y nunca la guarda
 */
public record NewUser(String email, String rawPassword, String firstName, String lastName, String phone) {

    /** Evita que la contraseña acabe en un log si alguien imprime el objeto. */
    @Override
    public String toString() {
        return "NewUser[email=" + email + ", firstName=" + firstName + ", lastName=" + lastName + "]";
    }

}
