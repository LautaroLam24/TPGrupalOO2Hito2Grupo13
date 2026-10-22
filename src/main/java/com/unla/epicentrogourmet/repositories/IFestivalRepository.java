package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Festival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository("festivalRepository")
public interface IFestivalRepository extends JpaRepository<Festival, Integer> {

    List<Festival> findByFechaFinGreaterThanEqualOrderByFechaInicioAsc(LocalDate fecha);
}
