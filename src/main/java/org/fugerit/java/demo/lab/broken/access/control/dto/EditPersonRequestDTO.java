package org.fugerit.java.demo.lab.broken.access.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Richiesta per la modifica di una persona.
 *
 * <p>
 * NOTA DI SICUREZZA (field-level authorization): il campo {@code minRole} è un campo PRIVILEGIATO.
 * La presenza del campo nel DTO non implica che chiunque possa valorizzarlo: l'autorizzazione a
 * livello di campo viene applicata lato server in {@code DocResource.editPerson} (solo il ruolo
 * 'admin' può modificare {@code minRole}). La validazione Bean qui sotto è un primo filtro
 * (whitelist dei valori ammessi), ma NON sostituisce il controllo di autorizzazione.
 * </p>
 */
@Getter
@Setter
@Schema(description = "Richiesta per il servizio di modifica di una persona")
public class EditPersonRequestDTO {

    private static final String NAME_PATTERN = "^[\\p{L}\\s-]+$";
    private static final String NAME_MESSAGE = "Può contenere solo lettere, spazi e trattini";

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 512, message = "Il nome non può superare i 512 caratteri")
    @Pattern(regexp = NAME_PATTERN, message = "Il nome: " + NAME_MESSAGE)
    @Schema(description = "il nome della persona", examples = { "MARIE" }, required = true)
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 512, message = "Il cognome non può superare i 512 caratteri")
    @Pattern(regexp = NAME_PATTERN, message = "Il cognome: " + NAME_MESSAGE)
    @Schema(description = "il cognome della persona", examples = { "CURIE" }, required = true)
    private String lastName;

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 512, message = "Il titolo non può superare i 512 caratteri")
    @Pattern(regexp = NAME_PATTERN, message = "Il titolo: " + NAME_MESSAGE)
    @Schema(description = "il titolo della persona", examples = { "Fisica" }, required = true)
    private String title;

    @Size(max = 32, message = "Il ruolo minimo non può superare i 32 caratteri")
    @Pattern(regexp = "^(admin|user|guest)?$", message = "Il ruolo minimo deve essere: admin, user, guest oppure vuoto")
    @Schema(description = "CAMPO PRIVILEGIATO: ruolo minimo richiesto per visualizzare la persona. Modificabile solo dal ruolo 'admin'.", examples = {
            "guest" })
    private String minRole;

}
