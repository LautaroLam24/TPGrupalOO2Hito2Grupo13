package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IPersonalRepository extends JpaRepository<Personal, Integer> {
    List<Personal> findByBajaFalse();
    List<Personal> findByUserUsernameAndBajaFalse(String username);
    List<Personal> findByBajaTrue();
    boolean existsByDni(int dni);

    @Query("select p from UnidadDeVenta u join u.staff p "
            + "where u.responsable.username = :username and p.baja = :baja")
    List<Personal> findByResponsableYBaja(@Param("username") String username,
                                          @Param("baja") boolean baja);

    @Query("select p from UnidadDeVenta u join u.staff p "
            + "where u.responsable.username = :username and p.id = :id")
    Optional<Personal> findByIdYResponsable(@Param("username") String username,
                                            @Param("id") int id);
}