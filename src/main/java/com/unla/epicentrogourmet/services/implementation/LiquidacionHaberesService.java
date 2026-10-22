package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.LiquidacionHaberesDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.Personal;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiquidacionHaberesService {

    private final IPersonalRepository personalRepository;
    private final IFestivalRepository festivalRepository;

    public LiquidacionHaberesService(
            IPersonalRepository personalRepository,
            IFestivalRepository festivalRepository) {
        this.personalRepository = personalRepository;
        this.festivalRepository = festivalRepository;
    }

    @Transactional(readOnly = true)
    public LiquidacionHaberesDTO liquidar(
            String username, int festivalId) {

        Personal personal = personalRepository
                .findByUserUsernameAndBajaFalse(username)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró el empleado activo"));

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró el festival"));

        float sueldo = personal.calcularSueldo(festival);

        return new LiquidacionHaberesDTO(
                personal.getNombre() + " " + personal.getApellido(),
                festival.getNombre(),
                festival.calcularDuracionDias(),
                sueldo
        );
    }
}