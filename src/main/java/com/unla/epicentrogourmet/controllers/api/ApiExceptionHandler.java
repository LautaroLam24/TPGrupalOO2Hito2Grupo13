package com.unla.epicentrogourmet.controllers.api;

import com.unla.epicentrogourmet.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Manejo de errores común a todos los @RestController de la API.
 * Está limitado al paquete controllers.api para no afectar a los controllers de Thymeleaf.
 * Todas las respuestas de error tienen la forma {"error": "mensaje"}.
 */
@RestControllerAdvice(basePackages = "com.unla.epicentrogourmet.controllers.api")
public class ApiExceptionHandler {

    /** Validaciones de negocio de los services. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIllegalArgument(IllegalArgumentException ex) {
        return error(ex.getMessage());
    }

    /** Bean Validation del body (@Valid): lista cada campo con su problema. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(MethodArgumentNotValidException ex) {
        String campos = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error("Datos inválidos. " + campos);
    }

    /** Parámetro de la URL con formato inválido (fecha mal escrita, estado inexistente, etc.). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parámetro '" + ex.getName() + "' tiene un valor inválido: '" + ex.getValue() + "'.";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            mensaje += " Valores posibles: " + Arrays.toString(tipo.getEnumConstants()) + ".";
        } else if (LocalDate.class.equals(tipo)) {
            mensaje += " Formato esperado: yyyy-MM-dd.";
        }
        return error(mensaje);
    }

    /** Body que no es un JSON válido o tiene tipos incorrectos (por ejemplo, una fecha mal formada). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarBodyIlegible(HttpMessageNotReadableException ex) {
        return error("El cuerpo de la solicitud no es un JSON válido o tiene valores con formato "
                + "incorrecto (las fechas van como yyyy-MM-dd y los costos como números).");
    }

    private ResponseEntity<ErrorResponseDTO> error(String mensaje) {
        return new ResponseEntity<>(new ErrorResponseDTO(mensaje), HttpStatus.BAD_REQUEST);
    }
}
