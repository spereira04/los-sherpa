package com.lossherpa.error;

/**
 * Login fallido. Se traduce a 401 con un mensaje unico, igual para email inexistente y para
 * contrasena incorrecta: no se revela si la cuenta existe.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contrasena incorrectos");
    }
}
