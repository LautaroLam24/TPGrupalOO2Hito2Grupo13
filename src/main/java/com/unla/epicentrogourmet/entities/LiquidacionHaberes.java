package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "liquidacion_haberes", uniqueConstraints =
        @UniqueConstraint(columnNames = {"personal_id", "festival_id", "periodo"}))
public class LiquidacionHaberes {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false)
    private int diasTrabajados;

    @Column(nullable = false)
    private float sueldoBase;

    @Column(nullable = false)
    private float adicionales;

    @Column(name = "factor_liquidacion", nullable = false)
    private float factor;

    @Column(nullable = false)
    private float sueldoTotal;

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "enviada", nullable = false)
    private boolean enviada = false;
}
