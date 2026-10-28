package com.unla.epicentrogourmet.exceptions;

public class GestionPlatoException extends RuntimeException {

    public GestionPlatoException(String message) {
        super(message);
    }

    public GestionPlatoException(String message, Throwable cause) {
        super(message, cause);
    }
}
