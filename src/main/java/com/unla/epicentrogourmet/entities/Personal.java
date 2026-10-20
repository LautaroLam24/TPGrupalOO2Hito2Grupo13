package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public abstract class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, unique = true)
    private int dni;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    private boolean baja;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public int calcularEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public int calcularAntiguedad(LocalDate fecha) {
        return Period.between(fechaIngreso, fecha).getYears();
    }

    // ceil(dias/15) * 50%: respeta la tabla del enunciado y sus dos ejemplos
    protected float factorLiquidacion(Festival festival) {
        return (float) (Math.ceil(festival.calcularDuracionDias() / 15.0) * 0.5);
    }

    public abstract float calcularSueldo(Festival festival);
}