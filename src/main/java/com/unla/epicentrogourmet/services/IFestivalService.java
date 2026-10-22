package com.unla.epicentrogourmet.services;

import com.unla.epicentrogourmet.entities.Festival;

import java.util.List;

public interface IFestivalService {

    List<Festival> traerTodos();

    List<Festival> traerDisponibles();
}
