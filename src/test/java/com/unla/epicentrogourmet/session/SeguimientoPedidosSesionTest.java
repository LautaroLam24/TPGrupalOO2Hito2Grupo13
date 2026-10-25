package com.unla.epicentrogourmet.session;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeguimientoPedidosSesionTest {

    private static final String CAJERO_A = "cajero.a@epicentro.com";
    private static final String CAJERO_B = "cajero.b@epicentro.com";

    private final SeguimientoPedidosSesion seguimiento = new SeguimientoPedidosSesion();

    @Test
    void registraAcumulaYNoDuplicaIds() {
        MockHttpSession session = new MockHttpSession();

        seguimiento.registrar(session, CAJERO_A, 10);
        seguimiento.registrar(session, CAJERO_A, 20);
        seguimiento.registrar(session, CAJERO_A, 10);

        assertEquals(Set.of(10, 20), seguimiento.obtenerIds(session, CAJERO_A));
    }

    @Test
    void devuelveUnaCopiaDefensivaNoModificable() {
        MockHttpSession session = new MockHttpSession();
        seguimiento.registrar(session, CAJERO_A, 10);

        Set<Integer> ids = seguimiento.obtenerIds(session, CAJERO_A);

        assertThrows(UnsupportedOperationException.class, () -> ids.add(20));
        assertEquals(Set.of(10), seguimiento.obtenerIds(session, CAJERO_A));
    }

    @Test
    void aíslaUsuariosDentroDeLaMismaSesionYLimpiaSoloElActual() {
        MockHttpSession session = new MockHttpSession();
        seguimiento.registrar(session, CAJERO_A, 10);
        seguimiento.registrar(session, CAJERO_B, 20);

        seguimiento.limpiar(session, CAJERO_A);

        assertTrue(seguimiento.obtenerIds(session, CAJERO_A).isEmpty());
        assertEquals(Set.of(20), seguimiento.obtenerIds(session, CAJERO_B));
    }

    @Test
    void aíslaDosSesionesDelMismoUsuario() {
        MockHttpSession primera = new MockHttpSession();
        MockHttpSession segunda = new MockHttpSession();
        seguimiento.registrar(primera, CAJERO_A, 10);
        seguimiento.registrar(segunda, CAJERO_A, 20);

        assertEquals(Set.of(10), seguimiento.obtenerIds(primera, CAJERO_A));
        assertEquals(Set.of(20), seguimiento.obtenerIds(segunda, CAJERO_A));
    }

    @Test
    void despuesDeLimpiarUnAltaIniciaUnNuevoCiclo() {
        MockHttpSession session = new MockHttpSession();
        seguimiento.registrar(session, CAJERO_A, 10);
        seguimiento.limpiar(session, CAJERO_A);

        seguimiento.registrar(session, CAJERO_A, 30);

        assertEquals(Set.of(30), seguimiento.obtenerIds(session, CAJERO_A));
    }

    @Test
    void rechazaSesionUsernameEIdsInvalidos() {
        MockHttpSession session = new MockHttpSession();

        assertThrows(IllegalArgumentException.class,
                () -> seguimiento.registrar(null, CAJERO_A, 1));
        assertThrows(IllegalArgumentException.class,
                () -> seguimiento.registrar(session, " ", 1));
        assertThrows(IllegalArgumentException.class,
                () -> seguimiento.registrar(session, CAJERO_A, 0));
    }

    @Test
    void coordinaAltasConcurrentesBasicasEnLaMismaJvm() throws Exception {
        MockHttpSession session = new MockHttpSession();
        int cantidad = 40;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<?>> tareas = new ArrayList<>();

        try {
            for (int id = 1; id <= cantidad; id++) {
                int pedidoId = id;
                tareas.add(executor.submit(() -> {
                    inicio.await();
                    seguimiento.registrar(session, CAJERO_A, pedidoId);
                    seguimiento.registrar(session, CAJERO_A, pedidoId);
                    return null;
                }));
            }
            inicio.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(cantidad, seguimiento.obtenerIds(session, CAJERO_A).size());
    }
}
