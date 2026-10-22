package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.FoodTruck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("foodTruckRepository")
public interface IFoodTruckRepository extends JpaRepository<FoodTruck, Integer> {

    boolean existsByPatente(String patente);
}
