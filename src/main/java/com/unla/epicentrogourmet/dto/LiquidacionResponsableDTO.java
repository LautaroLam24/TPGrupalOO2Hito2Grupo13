package com.unla.epicentrogourmet.dto;

public class LiquidacionResponsableDTO {
    private int empleadoId;
    private int festivalId;
    private String periodo;
    private String empleado;
    private String email;
    private String festival;
    private int diasTrabajados;
    private int diasFestival;
    private float sueldoBase;
    private float adicionales;
    private float factor;
    private float sueldoTotal;

    public LiquidacionResponsableDTO(int empleadoId, int festivalId, String periodo, String empleado,
            String email, String festival, int diasTrabajados, int diasFestival, float sueldoBase,
            float adicionales, float factor, float sueldoTotal) {
        this.empleadoId=empleadoId; this.festivalId=festivalId; this.periodo=periodo;
        this.empleado=empleado; this.email=email; this.festival=festival; this.diasTrabajados=diasTrabajados;
        this.diasFestival=diasFestival; this.sueldoBase=sueldoBase; this.adicionales=adicionales;
        this.factor=factor; this.sueldoTotal=sueldoTotal;
    }
    public int getEmpleadoId(){return empleadoId;} public int getFestivalId(){return festivalId;}
    public String getPeriodo(){return periodo;} public String getEmpleado(){return empleado;}
    public String getEmail(){return email;} public String getFestival(){return festival;}
    public int getDiasTrabajados(){return diasTrabajados;} public int getDiasFestival(){return diasFestival;}
    public float getSueldoBase(){return sueldoBase;} public float getAdicionales(){return adicionales;}
    public float getFactor(){return factor;} public float getSueldoTotal(){return sueldoTotal;}
}
