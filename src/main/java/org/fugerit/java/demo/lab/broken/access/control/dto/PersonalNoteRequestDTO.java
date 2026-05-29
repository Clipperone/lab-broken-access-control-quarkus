package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Richiesta per creare/modificare una nota personale (SCENARIO 1, ownership).
 *
 * <p>
 * Nota: il proprietario NON è un campo del DTO. L'owner è ricavato dall'identità autenticata lato
 * server; il client non può dichiararsi proprietario di una nota.
 * </p>
 */
@Getter
@Setter
@Schema(description = "Richiesta per creare/modificare una nota personale")
public class PersonalNoteRequestDTO {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 512, message = "Il titolo non può superare i 512 caratteri")
    @Schema(description = "Titolo della nota", required = true)
    private String title;

    @Size(max = 4000, message = "Il contenuto non può superare i 4000 caratteri")
    @Schema(description = "Contenuto della nota")
    private String content;

}
