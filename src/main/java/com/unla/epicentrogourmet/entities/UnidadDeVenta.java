package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public abstract class UnidadDeVenta {

    private static final Pattern PATRON_CODIGO =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{10}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombreComercial;

    @Column(name = "superficie_m2")
    private float superficieM2;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "responsable_id", unique = true)
    private User responsable;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "unidad_id")
    private Set<Plato> platos = new HashSet<>();

    @OneToMany
    @JoinColumn(name = "unidad_id")
    private Set<Personal> staff = new HashSet<>();

    @OneToMany
    @JoinColumn(name = "unidad_id")
    private Set<Pedido> pedidos = new HashSet<>();

    public static boolean validarCodigo(String codigo) {
        return codigo != null && PATRON_CODIGO.matcher(codigo).matches();
    }

    public abstract float calcularCanon(Festival festival);

    public abstract String getTipo();
}
