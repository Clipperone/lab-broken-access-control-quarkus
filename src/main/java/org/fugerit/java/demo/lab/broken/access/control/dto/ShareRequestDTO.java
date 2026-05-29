package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Richiesta di condivisione esplicita di un documento di ufficio con un altro utente.
 */
@Getter
@Setter
@Schema(description = "Richiesta di condivisione di un documento con un altro utente")
public class ShareRequestDTO {

    @NotBlank(message = "L'upn del destinatario è obbligatorio")
    @Size(max = 128, message = "L'upn non può superare i 128 caratteri")
    @Schema(description = "upn dell'utente con cui condividere il documento", required = true)
    private String targetUpn;

}
