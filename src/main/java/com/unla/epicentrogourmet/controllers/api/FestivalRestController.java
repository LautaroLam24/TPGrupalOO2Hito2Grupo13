package com.unla.epicentrogourmet.controllers.api;

import com.unla.epicentrogourmet.dto.ErrorResponseDTO;
import com.unla.epicentrogourmet.dto.EstadoFestival;
import com.unla.epicentrogourmet.dto.FestivalRequestDTO;
import com.unla.epicentrogourmet.dto.FestivalResponseDTO;
import com.unla.epicentrogourmet.services.IFestivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/festivales")
@Tag(name = "Festivales", description = "Consulta y alta de festivales")
public class FestivalRestController {

    private final IFestivalService festivalService;

    public FestivalRestController(IFestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar festivales",
            description = "Devuelve los festivales ordenados por fecha de inicio. Todos los filtros son "
                    + "opcionales: nombre, rango desde/hasta y estado. Sin filtros devuelve todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de festivales",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = FestivalResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "La fecha desde es posterior a la fecha hasta",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{\"error\": \"La fecha 'desde' (2026-12-31) "
                                    + "no puede ser posterior a la fecha 'hasta' (2026-01-01).\"}")))
    })
    public ResponseEntity<List<FestivalResponseDTO>> listar(
            @Parameter(description = "Parte del nombre del festival", example = "cerveza")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Fecha desde (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Estado del festival: EN_CURSO, PROXIMO o FINALIZADO", example = "EN_CURSO")
            @RequestParam(required = false) EstadoFestival estado) {
        return new ResponseEntity<>(festivalService.filtrar(nombre, desde, hasta, estado), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear festival",
            description = "Crea un festival con sus costos. Valida que la fecha de fin no sea anterior a la "
                    + "de inicio, que el nombre no se repita y que los costos sean mayores a 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Festival creado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FestivalResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{\"error\": \"La fecha de fin no puede ser "
                                    + "anterior a la fecha de inicio.\"}")))
    })
    public ResponseEntity<FestivalResponseDTO> alta(@Valid @RequestBody FestivalRequestDTO festivalRequest) {
        return new ResponseEntity<>(festivalService.alta(festivalRequest), HttpStatus.CREATED);
    }
}
