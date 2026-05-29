package org.fugerit.java.demo.lab.broken.access.control.dto;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Schema(description = "Risposta del servizio documenti di ufficio")
public class OfficeDocumentResponseDTO {

    @Schema(description = "UUID del documento")
    private String uuid;

    @Schema(description = "Proprietario (upn)")
    private String ownerUpn;

    @Schema(description = "Ufficio del proprietario")
    private String ownerOffice;

    @Schema(description = "Ruolo soglia (ruolo dell'owner al caricamento)")
    private String ownerRole;

    @Schema(description = "Stato: DRAFT o PUBLISHED")
    private String status;

    @Schema(description = "Nome del file")
    private String fileName;

    @Schema(description = "Contenuto testuale")
    private String content;

    @Schema(description = "Data di creazione")
    private LocalDateTime creationDate;

    @Schema(description = "upn con cui il documento è condiviso esplicitamente")
    private Set<String> sharedWith;

}
