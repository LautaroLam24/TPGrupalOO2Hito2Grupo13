package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Festival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository("festivalRepository")
public interface IFestivalRepository extends JpaRepository<Festival, Integer> {

    List<Festival> findByFechaFinGreaterThanEqualOrderByFechaInicioAsc(LocalDate fecha);

    @Query("select distinct f from Festival f join f.unidades u where u.id = :unidadId")
    List<Festival> findByUnidadId(@Param("unidadId") int unidadId);
}
