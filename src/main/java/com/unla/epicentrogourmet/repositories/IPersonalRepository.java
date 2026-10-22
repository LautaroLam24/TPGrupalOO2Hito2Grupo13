package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPersonalRepository extends JpaRepository<Personal, Integer> {
    List<Personal> findByBajaFalse();
    List<Personal> findByUserUsernameAndBajaFalse(String username);
    boolean existsByDni(int dni);
}
