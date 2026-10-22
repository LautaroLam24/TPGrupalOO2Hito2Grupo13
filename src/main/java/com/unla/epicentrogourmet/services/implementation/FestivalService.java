package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.services.IFestivalService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service("festivalService")
public class FestivalService implements IFestivalService {

    private final IFestivalRepository festivalRepository;

    public FestivalService(IFestivalRepository festivalRepository) {
        this.festivalRepository = festivalRepository;
    }

    @Override
    public List<Festival> traerTodos() {
        return festivalRepository.findAll();
    }

    @Override
    public List<Festival> traerDisponibles() {
        return festivalRepository.findByFechaFinGreaterThanEqualOrderByFechaInicioAsc(LocalDate.now());
    }
}
