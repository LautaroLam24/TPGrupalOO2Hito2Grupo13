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
}
