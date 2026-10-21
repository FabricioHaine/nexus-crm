package com.nexus.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio das notificacoes do sistema.
 *
 * <p>Quando o SMTP nao esta configurado, a mensagem e registrada no log em
 * vez de ser enviada — assim o fluxo de recuperacao de senha continua
 * utilizavel em desenvolvimento, sem depender de um servidor de e-mail.</p>
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public NotificacaoService(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${app.mail.remetente:nao-responda@nexus-crm.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    /**
     * Envia o link de recuperacao de senha.
     */
    public void enviarLinkDeRecuperacao(String destinatario, String nome, String link) {
        String corpo = "Ola, " + nome + "!\n\n"
                + "Recebemos um pedido de redefinicao da sua senha no Nexus CRM.\n"
                + "Clique no link abaixo para escolher uma nova senha:\n\n"
                + link + "\n\n"
                + "O link expira em uma hora e pode ser usado uma unica vez.\n"
                + "Se voce nao pediu isso, basta ignorar esta mensagem.\n";

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP nao configurado: o link de recuperacao de {} seria {}. "
                    + "Configure spring.mail.host para habilitar o envio real.", destinatario, link);
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject("Redefinicao de senha - Nexus CRM");
            mensagem.setText(corpo);
            sender.send(mensagem);
            log.info("Link de recuperacao enviado para {}.", destinatario);
        } catch (RuntimeException ex) {
            log.error("Falha ao enviar o link de recuperacao para {}", destinatario, ex);
        }
    }
}
