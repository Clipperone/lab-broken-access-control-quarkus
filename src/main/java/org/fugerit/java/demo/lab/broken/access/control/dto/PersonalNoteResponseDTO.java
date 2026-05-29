package org.fugerit.java.demo.lab.broken.access.control.dto;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Risposta del servizio note personali")
public class PersonalNoteResponseDTO {

    @Schema(description = "UUID della nota")
    private String uuid;

    @Schema(description = "Proprietario (upn) della nota")
    private String ownerUpn;

    @Schema(description = "Titolo della nota")
    private String title;

    @Schema(description = "Contenuto della nota")
    private String content;

    @Schema(description = "Data di creazione")
    private LocalDateTime creationDate;

}
