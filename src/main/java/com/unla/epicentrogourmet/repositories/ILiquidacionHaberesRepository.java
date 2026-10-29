package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.LiquidacionHaberes;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ILiquidacionHaberesRepository extends JpaRepository<LiquidacionHaberes, Long> {
    List<LiquidacionHaberes> findByPersonalUserUsernameOrderByPeriodoDesc(String username);
    Optional<LiquidacionHaberes> findByIdAndPersonalUserUsername(Long id, String username);
    Optional<LiquidacionHaberes> findByPersonalIdAndFestivalIdAndPeriodo(int personalId, int festivalId, String periodo);
}
