package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("unidadDeVentaRepository")
public interface IUnidadDeVentaRepository extends JpaRepository<UnidadDeVenta, Integer> {

    boolean existsByCodigo(String codigo);
}
