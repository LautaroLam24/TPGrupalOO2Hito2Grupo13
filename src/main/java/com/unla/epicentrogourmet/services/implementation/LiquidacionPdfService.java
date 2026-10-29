package com.unla.epicentrogourmet.services.implementation;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfWriter;
import com.unla.epicentrogourmet.entities.LiquidacionHaberes;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

@Service
public class LiquidacionPdfService {
    public byte[] generar(LiquidacionHaberes l) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, salida);
            documento.open();
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            documento.add(new Paragraph("Epicentro Gourmet - Liquidacion de haberes", titulo));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Empleado: " + l.getPersonal().getNombre() + " " + l.getPersonal().getApellido()));
            documento.add(new Paragraph("Periodo: " + l.getPeriodo()));
            documento.add(new Paragraph("Festival: " + l.getFestival().getNombre()));
            documento.add(new Paragraph("Dias trabajados en el periodo: " + l.getDiasTrabajados()));
            documento.add(new Paragraph("Fecha de envio: " + l.getFechaEnvio()));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Detalle del calculo", subtitulo));
            documento.add(new Paragraph(String.format("Sueldo base: $ %.2f", l.getSueldoBase())));
            documento.add(new Paragraph(String.format("Adicionales: $ %.2f", l.getAdicionales())));
            documento.add(new Paragraph(String.format("Proporción según días trabajados: %.4f", l.getFactor())));
            documento.add(new Paragraph(String.format("Cálculo: (sueldo base + adicionales) x proporción trabajada = $ %.2f", l.getSueldoTotal())));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(String.format("TOTAL A COBRAR: $ %.2f", l.getSueldoTotal()), subtitulo));
            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la liquidacion.", e);
        }
    }
}
