package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Festival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("festivalRepository")
public interface IFestivalRepository extends JpaRepository<Festival, Integer> {
}