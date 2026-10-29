package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.LiquidacionResponsableDTO;
import com.unla.epicentrogourmet.entities.*;
import com.unla.epicentrogourmet.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LiquidacionResponsableService {
    private final IPersonalRepository personalRepository;
    private final IFestivalRepository festivalRepository;
    private final IUnidadDeVentaRepository unidadRepository;
    private final ILiquidacionHaberesRepository liquidacionRepository;

    public LiquidacionResponsableService(IPersonalRepository personalRepository, IFestivalRepository festivalRepository,
            IUnidadDeVentaRepository unidadRepository, ILiquidacionHaberesRepository liquidacionRepository) {
        this.personalRepository=personalRepository; this.festivalRepository=festivalRepository;
        this.unidadRepository=unidadRepository; this.liquidacionRepository=liquidacionRepository;
    }

    @Transactional(readOnly = true)
    public List<Personal> empleadosDelResponsable(String username) {
        return personalRepository.findByResponsableYBaja(username, false);
    }

    @Transactional(readOnly = true)
    public List<Festival> festivalesDelResponsable(String username) {
        UnidadDeVenta unidad = unidadRepository.findByResponsableUsername(username).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la unidad del responsable."));
        return festivalRepository.findByUnidadId(unidad.getId()).stream()
                .filter(f -> f.getFechaFin() != null && f.getFechaInicio() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public LiquidacionResponsableDTO calcular(String username, int empleadoId, int festivalId, String periodo) {
        Personal personal = personalRepository.findByIdYResponsable(username, empleadoId)
                .filter(p -> !p.isBaja()).orElseThrow(() -> new IllegalArgumentException(
                        "El empleado no pertenece a tu unidad o está dado de baja."));
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el festival."));
        UnidadDeVenta unidad = unidadRepository.findByResponsableUsername(username).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la unidad del responsable."));
        if (!unidad.getStaff().stream().anyMatch(p -> p.getId() == personal.getId())
                || !festival.getUnidades().stream().anyMatch(u -> u.getId() == unidad.getId())) {
            throw new IllegalArgumentException("El empleado no trabajó en un festival de tu unidad.");
        }
        YearMonth ym;
        try { ym = YearMonth.parse(periodo); }
        catch (Exception ex) { throw new IllegalArgumentException("Seleccioná un período mensual válido."); }
        LocalDate desde = festival.getFechaInicio().isAfter(ym.atDay(1)) ? festival.getFechaInicio() : ym.atDay(1);
        LocalDate hasta = festival.getFechaFin().isBefore(ym.atEndOfMonth()) ? festival.getFechaFin() : ym.atEndOfMonth();
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("El empleado no trabajó durante el mes seleccionado.");
        int dias = (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
        int diasFestival = festival.calcularDuracionDias();
        float base = festival.getCosto().getSueldoBase();
        float adicionales = 0;
        if (personal instanceof Cajero cajero) {
            adicionales = cajero.calcularAntiguedad(festival.getFechaInicio()) * festival.getCosto().getPlusAntiguedad();
        } else if (personal instanceof EmpleadoCocina cocina) {
            adicionales = switch (cocina.getCategoria()) {
                case COCINERO -> festival.getCosto().getPlusCocinero();
                case AYUDANTE -> festival.getCosto().getPlusAyudante();
                case LAVAPLATOS -> festival.getCosto().getPlusLavaplatos();
            };
        }
        float factorFestival = (float)(Math.ceil(diasFestival / 15.0) * 0.5);
        float factorMensual = factorFestival * ((float)dias / diasFestival);
        float total = (base + adicionales) * factorMensual;
        return new LiquidacionResponsableDTO(personal.getId(), festival.getId(), periodo,
                personal.getNombre()+" "+personal.getApellido(), personal.getUser().getUsername(), festival.getNombre(),
                dias, diasFestival, base, adicionales, factorMensual, total);
    }

    @Transactional
    public LiquidacionHaberes guardarEnviada(String username, LiquidacionResponsableDTO dto) {
        // Revalida pertenencia y período antes de guardar para no confiar en los datos del formulario.
        LiquidacionResponsableDTO calculada = calcular(username, dto.getEmpleadoId(), dto.getFestivalId(), dto.getPeriodo());
        LiquidacionHaberes existente = liquidacionRepository.findByPersonalIdAndFestivalIdAndPeriodo(
                calculada.getEmpleadoId(), calculada.getFestivalId(), calculada.getPeriodo()).orElse(null);
        if (existente != null) throw new IllegalArgumentException("Esta liquidación ya fue enviada para ese empleado, festival y mes.");
        Personal personal = personalRepository.findById(calculada.getEmpleadoId()).orElseThrow();
        Festival festival = festivalRepository.findById(calculada.getFestivalId()).orElseThrow();
        LiquidacionHaberes l = new LiquidacionHaberes();
        l.setPersonal(personal); l.setFestival(festival); l.setPeriodo(calculada.getPeriodo());
        l.setDiasTrabajados(calculada.getDiasTrabajados()); l.setSueldoBase(calculada.getSueldoBase());
        l.setAdicionales(calculada.getAdicionales()); l.setFactor(calculada.getFactor());
        l.setSueldoTotal(calculada.getSueldoTotal());
        l.setFechaEnvio(java.time.LocalDateTime.now());
        return liquidacionRepository.save(l);
    }

    @Transactional
    public void marcarComoEnviada(Long liquidacionId) {
        LiquidacionHaberes liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la liquidación."));
        liquidacion.setEnviada(true);
        liquidacionRepository.save(liquidacion);
    }

    @Transactional(readOnly = true)
    public List<LiquidacionHaberes> liquidacionesEmpleado(String username) {
        return liquidacionRepository.findByPersonalUserUsernameOrderByPeriodoDesc(username);
    }

    @Transactional(readOnly = true)
    public LiquidacionHaberes liquidacionVisibleParaEmpleado(Long id, String username) {
        return liquidacionRepository.findByIdAndPersonalUserUsername(id, username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró esa liquidación."));
    }
}
