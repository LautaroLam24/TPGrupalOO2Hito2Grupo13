package com.unla.epicentrogourmet.services;

import com.unla.epicentrogourmet.dto.EstadoFestival;
import com.unla.epicentrogourmet.dto.FestivalRequestDTO;
import com.unla.epicentrogourmet.dto.FestivalResponseDTO;
import com.unla.epicentrogourmet.dto.RankingUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteMayoresCanonDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IFestivalService {

    List<Festival> traerTodos();

    List<Festival> traerDisponibles();

    ReporteVentaDTO reporteRecaudacion(String username, int festivalId);

    List<Festival> traerTodosPorFecha();

    Optional<Festival> traerPorId(int idFestival);

    List<UnidadDeVenta> traerUnidades(int idFestival);

    List<RankingUnidadDTO> rankingUnidades(int idFestival);

    List<ReporteMayoresCanonDTO> unidadesMayorCanon(int idFestival, int x);

    // Los filtros nulos no filtran.
    List<FestivalResponseDTO> filtrar(String nombre, LocalDate desde, LocalDate hasta, EstadoFestival estado);

    FestivalResponseDTO alta(FestivalRequestDTO festivalRequest);
}
