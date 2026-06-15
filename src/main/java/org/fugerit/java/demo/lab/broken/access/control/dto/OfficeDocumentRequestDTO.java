package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Richiesta per creare/modificare un documento di ufficio (SCENARIO 3).
 *
 * <p>
 * IMPORTANTE (anti mass-assignment): questo DTO espone SOLO {@code fileName} e {@code content}. Owner,
 * ufficio, ruolo soglia e stato NON sono accettati dal client: vengono impostati lato server in base
 * all'identità autenticata. Eventuali campi extra nel JSON vengono ignorati.
 * </p>
 */
@Getter
@Setter
@Schema(description = "Richiesta per creare/modificare un documento di ufficio")
public class OfficeDocumentRequestDTO {

    @NotBlank(message = "Il nome file è obbligatorio")
    @Size(max = 512, message = "Il nome file non può superare i 512 caratteri")
    @Schema(description = "Nome del file", required = true)
    private String fileName;

    @Size(max = 4000, message = "Il contenuto non può superare i 4000 caratteri")
    @Schema(description = "Contenuto testuale del documento")
    private String content;

    // VULNERABILITY: (8e) questi campi sono server-managed e NON dovrebbero essere esposti nel contratto:
    // la loro presenza nel DTO abilita il mass assignment (il client può impostare owner/ufficio/ruolo/stato).
    // La soluzione è rimuoverli dal DTO e impostarli sempre lato server in base all'identità autenticata.
    @Schema(description = "(server-managed) owner del documento", hidden = true)
    private String ownerUpn;

    @Schema(description = "(server-managed) ufficio del documento", hidden = true)
    private String ownerOffice;

    @Schema(description = "(server-managed) ruolo soglia del documento", hidden = true)
    private String ownerRole;

    @Schema(description = "(server-managed) stato del documento", hidden = true)
    private String status;

}
