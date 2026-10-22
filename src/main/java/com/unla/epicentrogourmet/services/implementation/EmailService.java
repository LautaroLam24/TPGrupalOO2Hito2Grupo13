
package com.unla.epicentrogourmet.services.implementation;

import org.springframework.mail.SimpleMailMessage;
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
}