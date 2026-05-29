package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fugerit.java.demo.lab.broken.access.control.dto.OfficeDocumentResponseDTO;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity per lo SCENARIO 3 (multi-tenant + gerarchia): un documento txt appartiene a un owner e a un
 * ufficio. I campi {@code ownerUpn}, {@code ownerOffice}, {@code ownerRole} sono impostati lato server
 * dall'identità autenticata (mai dal client → niente mass assignment).
 */
@Entity
@Table(name = "OFFICE_DOCUMENT", schema = "LAB_BAC")
@Getter
@Setter
@ToString
public class OfficeDocument extends PanacheEntityBase {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    @Id
    @SequenceGenerator(name = "officeDocumentSeqGenerator", sequenceName = "LAB_BAC.SEQ_OFFICE_DOCUMENT", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "officeDocumentSeqGenerator")
    @Column(name = "ID")
    private Long id;

    @Column(name = "UUID", length = 64)
    private String uuid;

    @Column(name = "OWNER_UPN", length = 128)
    private String ownerUpn;

    @Column(name = "OWNER_OFFICE", length = 128)
    private String ownerOffice;

    /** Ruolo dell'owner al momento del caricamento: soglia minima per la lettura nello stesso ufficio. */
    @Column(name = "OWNER_ROLE", length = 32)
    private String ownerRole;

    @Column(name = "STATUS", length = 16)
    private String status;

    @Column(name = "FILE_NAME", length = 512)
    private String fileName;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    /** Condivisione esplicita: upn autorizzati alla lettura a prescindere da ufficio/ruolo. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "OFFICE_DOCUMENT_SHARED_WITH", schema = "LAB_BAC", joinColumns = @JoinColumn(name = "OFFICE_DOCUMENT_ID"))
    @Column(name = "SHARED_UPN", length = 128)
    private Set<String> sharedWith = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public OfficeDocumentResponseDTO toDTO() {
        OfficeDocumentResponseDTO dto = new OfficeDocumentResponseDTO();
        dto.setUuid(this.uuid);
        dto.setOwnerUpn(this.ownerUpn);
        dto.setOwnerOffice(this.ownerOffice);
        dto.setOwnerRole(this.ownerRole);
        dto.setStatus(this.status);
        dto.setFileName(this.fileName);
        dto.setContent(this.content);
        dto.setCreationDate(this.creationDate);
        dto.setSharedWith(new HashSet<>(this.sharedWith));
        return dto;
    }

}
