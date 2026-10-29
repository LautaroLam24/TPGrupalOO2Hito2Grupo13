package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.AltaPersonalForm;
import com.unla.epicentrogourmet.dto.CredencialesAltaDTO;
import com.unla.epicentrogourmet.entities.Cajero;
import com.unla.epicentrogourmet.entities.EmpleadoCocina;
import com.unla.epicentrogourmet.entities.Personal;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
import com.unla.epicentrogourmet.repositories.IRoleRepository;
import com.unla.epicentrogourmet.repositories.IUserRepository;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import com.unla.epicentrogourmet.dto.ModificarPersonalForm;
import com.unla.epicentrogourmet.dto.HistorialFestivalDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import java.text.Normalizer;

import com.unla.epicentrogourmet.dto.EmpleadoListadoDTO;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalService {

    private final IPersonalRepository personalRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IUnidadDeVentaRepository unidadDeVentaRepository;
    private final IFestivalRepository festivalRepository;

    private static final String CARACTERES =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";

    private static final SecureRandom RANDOM = new SecureRandom();

    public PersonalService(
            IPersonalRepository personalRepository,
            IUserRepository userRepository,
            IRoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
        IUnidadDeVentaRepository unidadDeVentaRepository,
        IFestivalRepository festivalRepository) {

        this.personalRepository = personalRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.unidadDeVentaRepository = unidadDeVentaRepository;
        this.festivalRepository = festivalRepository;
    }

    @Transactional
     public CredencialesAltaDTO alta(AltaPersonalForm form, String usernameResponsable)  {

        validarDatos(form);

         UnidadDeVenta unidad = unidadDeVentaRepository
        .findByResponsableUsername(usernameResponsable).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
                  "No se encontró una unidad de venta para el responsable autenticado."));

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (personalRepository.existsByDni(form.getDni())) {
            throw new IllegalArgumentException(
                    "Ya existe un empleado con ese DNI.");
        }

        if (userRepository.findByUsername(email).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con ese email.");
        }

        validarTipoEmpleado(form);

        Role rolEmpleado = roleRepository
                .findByRole(RoleName.ROLE_EMPLEADO)
                .orElseThrow(() -> new IllegalStateException(
                        "No está configurado el rol de empleado."));

        // Generar la contraseña temporal.
        String passwordTemporal = generarPassword(12);

        // Guardar la contraseña encriptada
        User usuario = new User(
                email,
                passwordEncoder.encode(passwordTemporal),
                true,
                rolEmpleado
        );

        Personal personal = crearPersonal(form, usuario);

        try {
            personalRepository.saveAndFlush(personal);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "No se pudo registrar el empleado. "
                            + "Verificá que el DNI y el email no estén repetidos.");
        }

                // Asignar el nuevo empleado a la unidad del responsable
        unidad.getStaff().add(personal);
        unidadDeVentaRepository.save(unidad);
        // Se devuelve la contraseña original para poder enviarla por email.
        return new CredencialesAltaDTO(
                personal.getNombre() + " " + personal.getApellido(),
                email,
                passwordTemporal
        );
    }

    private String generarPassword(int longitud) {

        StringBuilder password = new StringBuilder(longitud);

        for (int i = 0; i < longitud; i++) {
            int indice = RANDOM.nextInt(CARACTERES.length());
            password.append(CARACTERES.charAt(indice));
        }

        return password.toString();
    }

    private void validarDatos(AltaPersonalForm form) {

        if (form == null) {
            throw new IllegalArgumentException(
                    "Los datos del empleado son obligatorios.");
        }

        if (form.getNombre() == null || form.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio.");
        }

        if (form.getApellido() == null || form.getApellido().isBlank()) {
            throw new IllegalArgumentException(
                    "El apellido es obligatorio.");
        }

        if (form.getDni() == null || form.getDni() <= 0) {
            throw new IllegalArgumentException(
                    "El DNI debe ser un número positivo.");
        }

        if (form.getFechaNacimiento() == null) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento es obligatoria.");
        }

        if (form.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento no puede ser futura.");
        }

        if (form.getFechaNacimiento().plusYears(18)
                .isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "El empleado debe tener al menos 18 años.");
        }

        if (form.getFechaIngreso() == null) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso es obligatoria.");
        }

        if (form.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso no puede ser futura.");
        }

        if (form.getFechaIngreso().isBefore(form.getFechaNacimiento())) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso no puede ser anterior "
                            + "a la fecha de nacimiento.");
        }

        if (form.getEmail() == null
                || !form.getEmail().trim().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "Debe ingresar un email válido.");
        }

        if (form.getEmail().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El email no puede superar los 100 caracteres.");
        }
    }

    private void validarTipoEmpleado(AltaPersonalForm form) {

        if ("CAJERO".equals(form.getTipoEmpleado())) {

            if (form.getTurno() == null) {
                throw new IllegalArgumentException(
                        "Debe seleccionar el turno del cajero.");
            }

            return;
        }

        if ("COCINA".equals(form.getTipoEmpleado())) {

            if (form.getCategoria() == null) {
                throw new IllegalArgumentException(
                        "Debe seleccionar la categoría de cocina.");
            }

            return;
        }

        throw new IllegalArgumentException(
                "Debe seleccionar un tipo de empleado válido.");
    }

    private Personal crearPersonal(
            AltaPersonalForm form, User usuario) {

        Personal personal;

        if ("CAJERO".equals(form.getTipoEmpleado())) {

            Cajero cajero = new Cajero();
            cajero.setTurno(form.getTurno());
            personal = cajero;

        } else {

            EmpleadoCocina empleadoCocina = new EmpleadoCocina();
            empleadoCocina.setCategoria(form.getCategoria());

            empleadoCocina.setEspecialidad(
                    form.getEspecialidad() == null
                            ? null
                            : form.getEspecialidad().trim());

            personal = empleadoCocina;
        }

        personal.setNombre(form.getNombre().trim());
        personal.setApellido(form.getApellido().trim());
        personal.setDni(form.getDni());
        personal.setFechaNacimiento(form.getFechaNacimiento());
        personal.setFechaIngreso(form.getFechaIngreso());
        personal.setBaja(false);
        personal.setUser(usuario);

        return personal;
    }

        @Transactional(readOnly = true)
    public List<EmpleadoListadoDTO> filtrar(String username, String texto, String tipo) {
        return filtrarPorEstado(username, texto, tipo, false);
    }

    @Transactional(readOnly = true)
    public List<EmpleadoListadoDTO> filtrarInactivos(String username, String texto, String tipo) {
        return filtrarPorEstado(username, texto, tipo, true);
    }

    private List<EmpleadoListadoDTO> filtrarPorEstado(
            String username, String texto, String tipo, boolean inactivos) {

        String busqueda = normalizar(texto);
        String tipoBuscado = tipo == null ? "" : tipo.trim();

        return personalRepository.findByResponsableYBaja(username, inactivos).stream()
                .filter(p -> tipoBuscado.isEmpty() || tipoDe(p).equals(tipoBuscado))
                .filter(p -> busqueda.isEmpty()
                        || normalizar(p.getNombre() + " " + p.getApellido()).contains(busqueda)
                        || String.valueOf(p.getDni()).contains(busqueda))
                .sorted(Comparator.comparing(Personal::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Personal::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aListadoDTO)
                .toList();
    }

    private String tipoDe(Personal p) {
        return p instanceof Cajero ? "CAJERO" : "COCINA";
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private EmpleadoListadoDTO aListadoDTO(Personal p) {
        String detalle;
        if (p instanceof Cajero cajero) {
            detalle = "Turno " + cajero.getTurno();
        } else if (p instanceof EmpleadoCocina cocina) {
            detalle = String.valueOf(cocina.getCategoria());
        } else {
            detalle = "";
        }

        return new EmpleadoListadoDTO(
                p.getId(),
                p.getApellido() + ", " + p.getNombre(),
                p.getDni(),
                p instanceof Cajero ? "Cajero" : "Cocina",
                detalle,
                p.getFechaIngreso(),
                p.calcularAntiguedad(LocalDate.now()),
                p.getUser() != null ? p.getUser().getUsername() : "");
    }

    private Personal buscarDelResponsable(String username, int id) {
        return personalRepository.findByIdYResponsable(username, id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El empleado no existe o no pertenece a tu unidad."));
    }

    @Transactional
    public String baja(String username, int id) {
        Personal personal = buscarDelResponsable(username, id);

        if (personal.isBaja()) {
            throw new IllegalArgumentException(
                    "El empleado ya está dado de baja.");
        }

        personal.setBaja(true);
        personal.getUser().setEnabled(false);
        personalRepository.save(personal);

        return personal.getApellido() + ", " + personal.getNombre();
    }

    @Transactional
    public String reactivar(String username, int id) {
        Personal personal = buscarDelResponsable(username, id);

        if (!personal.isBaja()) {
            throw new IllegalArgumentException(
                    "El empleado ya está activo.");
        }

        personal.setBaja(false);
        personal.getUser().setEnabled(true);
        personalRepository.save(personal);

        return personal.getApellido() + ", " + personal.getNombre();
    }

        private static final String PATRON_NOMBRE = "^\\p{L}+(?:[ '\\-]\\p{L}+)*$";

    @Transactional(readOnly = true)
    public ModificarPersonalForm obtenerParaModificar(String username, int id) {
        Personal personal = buscarDelResponsable(username, id);

        if (personal.isBaja()) {
            throw new IllegalArgumentException(
                    "No se puede modificar un empleado dado de baja. Reactivalo primero.");
        }

        ModificarPersonalForm form = new ModificarPersonalForm();
        form.setNombre(personal.getNombre());
        form.setApellido(personal.getApellido());
        form.setDni(personal.getDni());
        form.setFechaNacimiento(personal.getFechaNacimiento());
        form.setFechaIngreso(personal.getFechaIngreso());
        form.setTipoEmpleado(tipoDe(personal));
        form.setEmail(personal.getUser().getUsername());

        if (personal instanceof Cajero cajero) {
            form.setTurno(cajero.getTurno());
        } else if (personal instanceof EmpleadoCocina cocina) {
            form.setCategoria(cocina.getCategoria());
            form.setEspecialidad(cocina.getEspecialidad());
        }

        return form;
    }

    @Transactional
    public String modificar(String username, int id, ModificarPersonalForm form) {
        Personal personal = buscarDelResponsable(username, id);

        if (personal.isBaja()) {
            throw new IllegalArgumentException(
                    "No se puede modificar un empleado dado de baja. Reactivalo primero.");
        }

        validarModificacion(id, form, personal);

        personal.setNombre(form.getNombre().trim());
        personal.setApellido(form.getApellido().trim());
        personal.setDni(form.getDni());
        personal.setFechaNacimiento(form.getFechaNacimiento());
        personal.setFechaIngreso(form.getFechaIngreso());

        if (personal instanceof Cajero cajero) {
            cajero.setTurno(form.getTurno());
        } else if (personal instanceof EmpleadoCocina cocina) {
            cocina.setCategoria(form.getCategoria());
            String especialidad = form.getEspecialidad();
            cocina.setEspecialidad(
                    especialidad == null || especialidad.isBlank() ? null : especialidad.trim());
        }

        try {
            personalRepository.saveAndFlush(personal);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "No se pudo modificar el empleado. Verificá que el DNI no esté repetido.");
        }

        return personal.getApellido() + ", " + personal.getNombre();
    }

    private void validarModificacion(int id, ModificarPersonalForm form, Personal personal) {

        // VALIDACIÓN DE CARGA 1: nombre y apellido solo con letras
        if (!form.getNombre().trim().matches(PATRON_NOMBRE)) {
            throw new IllegalArgumentException(
                    "El nombre solo puede contener letras, espacios, guiones y apóstrofes.");
        }

        if (!form.getApellido().trim().matches(PATRON_NOMBRE)) {
            throw new IllegalArgumentException(
                    "El apellido solo puede contener letras, espacios, guiones y apóstrofes.");
        }

        // VALIDACIÓN DE CARGA 2: DNI de 7 u 8 dígitos y no usado por otro empleado
        int dni = form.getDni();

        if (dni < 1_000_000 || dni > 99_999_999) {
            throw new IllegalArgumentException(
                    "El DNI debe tener entre 7 y 8 dígitos.");
        }

        if (personalRepository.existsByDniAndIdNot(dni, id)) {
            throw new IllegalArgumentException(
                    "Ya existe otro empleado con ese DNI.");
        }

        LocalDate hoy = LocalDate.now();

        if (form.getFechaNacimiento().plusYears(18).isAfter(hoy)) {
            throw new IllegalArgumentException(
                    "El empleado debe tener al menos 18 años.");
        }

        if (form.getFechaIngreso().isBefore(form.getFechaNacimiento())) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso no puede ser anterior a la fecha de nacimiento.");
        }

        if (personal instanceof Cajero && form.getTurno() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el turno del cajero.");
        }

        if (personal instanceof EmpleadoCocina && form.getCategoria() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar la categoría de cocina.");
        }
    }

        @Transactional(readOnly = true)
    public List<HistorialFestivalDTO> traerHistorial(String username) {
        Personal personal = personalRepository
                .findByUserUsernameAndBajaFalse(username).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el empleado activo."));

        UnidadDeVenta unidad = unidadDeVentaRepository.findByPersonal(personal).orElse(null);

        if (unidad == null) {
            return List.of();
        }

        LocalDate hoy = LocalDate.now();

        return festivalRepository.findByUnidadId(unidad.getId()).stream()
                .map(f -> new HistorialFestivalDTO(
                        f.getNombre(),
                        unidad.getNombreComercial(),
                        f.getFechaInicio(),
                        f.getFechaFin(),
                        estadoDe(f, hoy)))
                .toList();
    }

    private String estadoDe(Festival festival, LocalDate hoy) {
        if (festival.estaActivo(hoy)) {
            return "En curso";
        }
        return festival.getFechaFin().isBefore(hoy) ? "Finalizado" : "Próximo";
    }
}