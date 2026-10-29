package com.unla.epicentrogourmet.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnidadDeVentaTest {

    @Test
    void validarCodigoAceptaDiezAlfanumericosConLetraYNumero() {
        assertTrue(UnidadDeVenta.validarCodigo("ABCD123456"));
        assertTrue(UnidadDeVenta.validarCodigo("abcd123456"));
    }

    @Test
    void validarCodigoRechazaNuloYLongitudesInvalidas() {
        assertFalse(UnidadDeVenta.validarCodigo(null));
        assertFalse(UnidadDeVenta.validarCodigo("ABC123"));
        assertFalse(UnidadDeVenta.validarCodigo("ABCDEF12345"));
    }

    @Test
    void validarCodigoExigeAlMenosUnaLetraYUnNumero() {
        assertFalse(UnidadDeVenta.validarCodigo("ABCDEFGHIJ"));
        assertFalse(UnidadDeVenta.validarCodigo("1234567890"));
    }

    @Test
    void validarCodigoRechazaCaracteresNoAlfanumericos() {
        assertFalse(UnidadDeVenta.validarCodigo("ABCD-12345"));
        assertFalse(UnidadDeVenta.validarCodigo("ABCD 12345"));
    }

    @Test
    void cadaSubclaseInformaSuTipo() {
        assertEquals("Food truck", new FoodTruck().getTipo());
        assertEquals("Puesto desarmable", new PuestoDesarmable().getTipo());
    }

    @Test
    void canonDeFoodTruckConElectricidadSumaElPlus() {
        FoodTruck foodTruck = foodTruck(12f, true);
        Festival festival = festival(500f, 10f, 2000f);

        assertEquals(6000f, foodTruck.calcularCanonSuperficie(festival));
        assertEquals(2000f, foodTruck.calcularAjusteCanon(festival));
        assertEquals(8000f, foodTruck.calcularCanon(festival));
        assertEquals("Plus por conexión eléctrica", foodTruck.describirAjusteCanon(festival));
    }

    @Test
    void canonDeFoodTruckSinElectricidadEsSoloLaSuperficie() {
        FoodTruck foodTruck = foodTruck(12f, false);
        Festival festival = festival(500f, 10f, 2000f);

        assertEquals(0f, foodTruck.calcularAjusteCanon(festival));
        assertEquals(6000f, foodTruck.calcularCanon(festival));
        assertEquals("Sin conexión eléctrica", foodTruck.describirAjusteCanon(festival));
    }

    @Test
    void canonDePuestoDescuentaElMontaje() {
        PuestoDesarmable puesto = new PuestoDesarmable();
        puesto.setSuperficieM2(20f);
        puesto.setTiempoMontajeMin(90);
        Festival festival = festival(500f, 10f, 2000f);

        assertEquals(10000f, puesto.calcularCanonSuperficie(festival));
        assertEquals(-900f, puesto.calcularAjusteCanon(festival));
        assertEquals(9100f, puesto.calcularCanon(festival));
        assertEquals("Descuento por montaje (90 min)", puesto.describirAjusteCanon(festival));
    }

    @Test
    void canonUsaLosCostosDelFestival() {
        assertEquals(9700f, foodTruck(12f, true).calcularCanon(festival(600f, 10f, 2500f)));
    }

    private FoodTruck foodTruck(float superficie, boolean requiereElectricidad) {
        FoodTruck foodTruck = new FoodTruck();
        foodTruck.setSuperficieM2(superficie);
        foodTruck.setRequiereElectricidad(requiereElectricidad);
        return foodTruck;
    }

    private Festival festival(float costoSuperficie, float costoMontaje, float plusElectricidad) {
        Costo costo = new Costo();
        costo.setCostoSuperficie(costoSuperficie);
        costo.setCostoMontaje(costoMontaje);
        costo.setPlusElectricidad(plusElectricidad);
        Festival festival = new Festival();
        festival.setCosto(costo);
        return festival;
    }
}
