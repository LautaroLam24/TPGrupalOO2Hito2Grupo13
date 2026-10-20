package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
public class Festival {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @ManyToMany
    @JoinTable(name = "festival_x_unidad",
            joinColumns = @JoinColumn(name = "festival_id"),
            inverseJoinColumns = @JoinColumn(name = "unidad_id"))
    private Set<UnidadDeVenta> unidades = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "costo_id", unique = true)
    private Costo costo;

    public int calcularDuracionDias() {
        return (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1; // incluye ambos días
    }

    public boolean estaActivo(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}