package org.fugerit.java.demo.lab.broken.access.control.dto;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Scienziato del registro (per la prenotazione di appuntamenti)")
public class ScientistResponseDTO {

    @Schema(description = "upn dello scienziato")
    private String upn;

    @Schema(description = "ufficio di appartenenza")
    private String office;

    @Schema(description = "nome visualizzato")
    private String displayName;

}
