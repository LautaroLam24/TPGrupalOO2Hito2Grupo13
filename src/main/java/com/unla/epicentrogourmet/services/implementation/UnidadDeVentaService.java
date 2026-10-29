package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.PuestoDesarmable;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.RegistroUnidadException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IFoodTruckRepository;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
import com.unla.epicentrogourmet.repositories.IRoleRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import com.unla.epicentrogourmet.repositories.IUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service("unidadDeVentaService")
public class UnidadDeVentaService {

    public static final String TIPO_FOOD_TRUCK = "FOOD_TRUCK";
    public static final String TIPO_PUESTO_DESARMABLE = "PUESTO_DESARMABLE";

    private final IUnidadDeVentaRepository unidadDeVentaRepository;
    private final IFoodTruckRepository foodTruckRepository;
    private final IFestivalRepository festivalRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IPersonalRepository personalRepository;

    public UnidadDeVentaService(IUnidadDeVentaRepository unidadDeVentaRepository,
                                IFoodTruckRepository foodTruckRepository,
                                IFestivalRepository festivalRepository,
                                IUserRepository userRepository,
                                IRoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                IPersonalRepository personalRepository) {
        this.unidadDeVentaRepository = unidadDeVentaRepository;
        this.foodTruckRepository = foodTruckRepository;
        this.festivalRepository = festivalRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.personalRepository = personalRepository;
    }

    // Unidad del usuario logueado: la que tiene a cargo (responsable) o la que lo tiene en su staff (empleado)
    @Transactional(readOnly = true)
    public Optional<UnidadDeVenta> traerUnidadDelUsuario(String username) {
        Optional<UnidadDeVenta> unidadResponsable = unidadDeVentaRepository
                .findByResponsableUsername(username).stream()
                .findFirst();
        if (unidadResponsable.isPresent()) {
            return unidadResponsable;
        }

        return personalRepository.findByUserUsernameAndBajaFalse(username).stream()
                .findFirst()
                .flatMap(unidadDeVentaRepository::findByPersonal);
    }

    @Transactional
    public void registrarConResponsable(RegistroUnidadResponsableForm form) {
        validarDatosComunes(form);

        String codigoNormalizado = form.getCodigo().toUpperCase(Locale.ROOT);
        if (!UnidadDeVenta.validarCodigo(codigoNormalizado)) {
            throw new RegistroUnidadException(
                    "El código debe tener 10 caracteres alfanuméricos e incluir al menos una letra y un número.");
        }

        String patenteNormalizada = validarDatosEspecificos(form);

        if (unidadDeVentaRepository.existsByCodigo(codigoNormalizado)) {
            throw new RegistroUnidadException("El código de unidad ya está registrado.");
        }
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            throw new RegistroUnidadException("El email del responsable ya está registrado.");
        }
        if (patenteNormalizada != null && foodTruckRepository.existsByPatente(patenteNormalizada)) {
            throw new RegistroUnidadException("La patente ya está registrada.");
        }

        Festival festival = festivalRepository.findById(form.getFestivalId())
                .orElseThrow(() -> new RegistroUnidadException("El festival seleccionado no existe."));
        if (festival.getFechaFin() == null || festival.getFechaFin().isBefore(LocalDate.now())) {
            throw new RegistroUnidadException("El festival seleccionado ya finalizó.");
        }

        Role roleResponsable = roleRepository.findByRole(RoleName.ROLE_RESPONSABLE)
                .orElseThrow(() -> new RegistroUnidadException("No está configurado el rol de responsable."));

        User responsable = new User(
                form.getUsername(),
                passwordEncoder.encode(form.getPassword()),
                true,
                roleResponsable);

        UnidadDeVenta unidad = crearUnidad(form, codigoNormalizado, patenteNormalizada, responsable);

        try {
            UnidadDeVenta unidadGuardada = unidadDeVentaRepository.saveAndFlush(unidad);
            festival.getUnidades().add(unidadGuardada);
            festivalRepository.saveAndFlush(festival);
        } catch (DataIntegrityViolationException ex) {
            throw new RegistroUnidadException(
                    "No se pudo registrar la unidad porque alguno de sus datos únicos ya existe.");
        }
    }

    private void validarDatosComunes(RegistroUnidadResponsableForm form) {
        if (form == null) {
            throw new RegistroUnidadException("Los datos del registro son obligatorios.");
        }
        if (form.getFestivalId() == null) {
            throw new RegistroUnidadException("Debe seleccionar un festival.");
        }
        if (form.getCodigo() == null || form.getCodigo().isBlank()) {
            throw new RegistroUnidadException("El código es obligatorio.");
        }
        if (form.getNombreComercial() == null || form.getNombreComercial().isBlank()) {
            throw new RegistroUnidadException("El nombre comercial es obligatorio.");
        }
        if (form.getSuperficieM2() == null || form.getSuperficieM2() <= 0) {
            throw new RegistroUnidadException("La superficie debe ser mayor que cero.");
        }
        if (form.getUsername() == null || form.getUsername().isBlank()) {
            throw new RegistroUnidadException("El email del responsable es obligatorio.");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new RegistroUnidadException("La contraseña es obligatoria.");
        }
        if (!TIPO_FOOD_TRUCK.equals(form.getTipoUnidad())
                && !TIPO_PUESTO_DESARMABLE.equals(form.getTipoUnidad())) {
            throw new RegistroUnidadException("Debe seleccionar un tipo de unidad válido.");
        }
    }

    private String validarDatosEspecificos(RegistroUnidadResponsableForm form) {
        if (TIPO_FOOD_TRUCK.equals(form.getTipoUnidad())) {
            if (form.getPatente() == null || form.getPatente().isBlank()) {
                throw new RegistroUnidadException("La patente es obligatoria para un Food Truck.");
            }
            String patenteNormalizada = form.getPatente().toUpperCase(Locale.ROOT);
            if (patenteNormalizada.length() > 10) {
                throw new RegistroUnidadException("La patente no puede superar los 10 caracteres.");
            }
            return patenteNormalizada;
        }

        if (form.getCantCarpas() == null || form.getCantCarpas() <= 0) {
            throw new RegistroUnidadException("La cantidad de carpas debe ser mayor que cero.");
        }
        if (form.getTiempoMontajeMin() == null || form.getTiempoMontajeMin() < 0) {
            throw new RegistroUnidadException("El tiempo de montaje debe ser mayor o igual a cero.");
        }
        return null;
    }

    private UnidadDeVenta crearUnidad(RegistroUnidadResponsableForm form,
                                      String codigoNormalizado,
                                      String patenteNormalizada,
                                      User responsable) {
        UnidadDeVenta unidad;
        if (TIPO_FOOD_TRUCK.equals(form.getTipoUnidad())) {
            FoodTruck foodTruck = new FoodTruck();
            foodTruck.setPatente(patenteNormalizada);
            foodTruck.setRequiereElectricidad(form.isRequiereElectricidad());
            unidad = foodTruck;
        } else {
            PuestoDesarmable puesto = new PuestoDesarmable();
            puesto.setCantCarpas(form.getCantCarpas());
            puesto.setTiempoMontajeMin(form.getTiempoMontajeMin());
            unidad = puesto;
        }

        unidad.setCodigo(codigoNormalizado);
        unidad.setNombreComercial(form.getNombreComercial());
        unidad.setSuperficieM2(form.getSuperficieM2());
        unidad.setResponsable(responsable);
        return unidad;
    }
}
