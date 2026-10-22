package com.unla.epicentrogourmet.dto;

public class LiquidacionHaberesDTO {
    private String empleado;
    private String festival;
    private int diasTrabajados;
    private float sueldoTotal;

    public LiquidacionHaberesDTO(
            String empleado,
            String festival,
            int diasTrabajados,
            float sueldoTotal) {
        this.empleado = empleado;
        this.festival = festival;
        this.diasTrabajados = diasTrabajados;
        this.sueldoTotal = sueldoTotal;
    }

    public String getEmpleado() {
        return empleado;
    }

    public String getFestival() {
        return festival;
    }

    public int getDiasTrabajados() {
        return diasTrabajados;
    }

    public float getSueldoTotal() {
        return sueldoTotal;
    }
}
