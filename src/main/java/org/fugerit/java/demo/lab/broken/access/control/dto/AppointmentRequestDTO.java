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
 * NOTA (anti mass-assignment): il creatore NON è un campo del DTO. {@code creatorUpn} è ricavato
 * dall'identità autenticata lato server; il client sceglie solo scienziato, ufficio, data e oggetto.
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

    // VULNERABILITY: (9i) 'office' è server-managed e NON dovrebbe essere esposto nel contratto: la sua presenza
    // abilita il mass assignment (il client sceglie l'ufficio, scavalcando il tenant). La soluzione è rimuoverlo e
    // derivare l'ufficio dallo scienziato lato server. Reso opzionale perché i test (identici a main) non lo inviano.
    @Size(max = 128, message = "Nome ufficio troppo lungo")
    @Schema(description = "(server-managed) ufficio dell'appuntamento", hidden = true)
    private String office;

    @NotNull(message = "La data dell'appuntamento è obbligatoria")
    @Future(message = "La data dell'appuntamento deve essere nel futuro")
    @Schema(description = "data e ora dell'appuntamento (ISO-8601)", examples = { "2030-01-15T10:30:00" }, required = true)
    private LocalDateTime appointmentAt;

    @Size(max = 512, message = "Oggetto troppo lungo")
    @Schema(description = "oggetto dell'appuntamento")
    private String subject;

    // VULNERABILITY: (9e) 'creatorUpn' è server-managed e NON dovrebbe essere esposto nel contratto: la sua
    // presenza nel DTO abilita il mass assignment (il client può impostare il creatore). La soluzione è
    // rimuoverlo e ricavare il creatore dall'identità autenticata lato server.
    @Schema(description = "(server-managed) creatore dell'appuntamento", hidden = true)
    private String creatorUpn;

}
