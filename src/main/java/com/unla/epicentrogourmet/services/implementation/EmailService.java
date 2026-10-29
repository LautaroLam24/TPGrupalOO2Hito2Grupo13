
package com.unla.epicentrogourmet.services.implementation;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCredenciales(
            String destinatario,
            String nombreEmpleado,
            String usuario,
            String password) {

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destinatario);
        mensaje.setSubject(
                "Epicentro Gourmet - Credenciales de acceso");

        mensaje.setText(
                "Hola " + nombreEmpleado + ",\n\n"
                        + "Tu usuario fue creado en Epicentro Gourmet.\n\n"
                        + "Tus credenciales de acceso son:\n\n"
                        + "Usuario: " + usuario + "\n"
                        + "Contraseña temporal: " + password + "\n\n"
                        + "Utilizá estas credenciales para iniciar sesión.\n\n"
                        + "Saludos,\n"
                        + "Equipo Epicentro Gourmet"
        );

        mailSender.send(mensaje);
    }

    public void enviarLiquidacion(String destinatario, String nombreEmpleado, String periodo, byte[] pdf) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject("Epicentro Gourmet - Liquidacion de haberes " + periodo);
            helper.setText("Hola " + nombreEmpleado + ",\n\nTu liquidacion de haberes del periodo " + periodo
                    + " ya esta disponible en Epicentro Gourmet. Adjuntamos el PDF con el detalle del calculo.\n\nSaludos,\nEquipo Epicentro Gourmet");
            helper.addAttachment("liquidacion-" + periodo + ".pdf", () -> new java.io.ByteArrayInputStream(pdf));
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new org.springframework.mail.MailSendException("No se pudo preparar el correo de liquidacion.", e);
        }
    }

}