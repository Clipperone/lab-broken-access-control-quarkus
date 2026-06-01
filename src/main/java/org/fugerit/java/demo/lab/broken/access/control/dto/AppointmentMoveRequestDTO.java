package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Richiesta di spostamento (reschedule) di un appuntamento: nuova data/ora.
 */
@Getter
@Setter
@Schema(description = "Richiesta di spostamento di un appuntamento")
public class AppointmentMoveRequestDTO {

    @NotNull(message = "La nuova data dell'appuntamento è obbligatoria")
    @Future(message = "La nuova data deve essere nel futuro")
    @Schema(description = "nuova data e ora dell'appuntamento (ISO-8601)", examples = {
            "2030-02-01T09:00:00" }, required = true)
    private LocalDateTime newAppointmentAt;

}
