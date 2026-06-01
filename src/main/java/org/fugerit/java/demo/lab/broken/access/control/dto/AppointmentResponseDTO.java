package org.fugerit.java.demo.lab.broken.access.control.dto;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Risposta del servizio appuntamenti")
public class AppointmentResponseDTO {

    @Schema(description = "UUID dell'appuntamento")
    private String uuid;

    @Schema(description = "upn di chi ha prenotato (creatore)")
    private String creatorUpn;

    @Schema(description = "upn dello scienziato destinatario")
    private String scientistUpn;

    @Schema(description = "ufficio dell'appuntamento")
    private String office;

    @Schema(description = "data e ora dell'appuntamento")
    private LocalDateTime appointmentAt;

    @Schema(description = "oggetto dell'appuntamento")
    private String subject;

    @Schema(description = "data di creazione")
    private LocalDateTime creationDate;

}
