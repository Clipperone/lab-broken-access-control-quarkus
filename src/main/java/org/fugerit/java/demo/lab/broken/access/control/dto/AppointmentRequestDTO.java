package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Richiesta per prenotare un appuntamento.
 *
 * <p>
 * NOTA (anti mass-assignment): né il creatore né l'ufficio sono campi del DTO. {@code creatorUpn} è
 * ricavato dall'identità autenticata; {@code office} è derivato lato server dal registro degli scienziati
 * in base allo {@code scientistUpn} (mai dal client). Il client sceglie solo scienziato, data e oggetto.
 * </p>
 */
@Getter
@Setter
@Schema(description = "Richiesta per prenotare un appuntamento")
public class AppointmentRequestDTO {

    @NotBlank(message = "Lo scienziato destinatario è obbligatorio")
    @Size(max = 128, message = "upn dello scienziato troppo lungo")
    @Schema(description = "upn dello scienziato destinatario", examples = { "BOHR" }, required = true)
    private String scientistUpn;

    @NotNull(message = "La data dell'appuntamento è obbligatoria")
    @Future(message = "La data dell'appuntamento deve essere nel futuro")
    @Schema(description = "data e ora dell'appuntamento (ISO-8601)", examples = { "2030-01-15T10:30:00" }, required = true)
    private LocalDateTime appointmentAt;

    @Size(max = 512, message = "Oggetto troppo lungo")
    @Schema(description = "oggetto dell'appuntamento")
    private String subject;

}
