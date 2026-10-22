package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("unidadDeVentaRepository")
public interface IUnidadDeVentaRepository extends JpaRepository<UnidadDeVenta, Integer> {

    boolean existsByCodigo(String codigo);

    @Query("select distinct u from UnidadDeVenta u join u.staff p where p = :personal")
    Optional<UnidadDeVenta> findByPersonal(@Param("personal") Personal personal);
}
