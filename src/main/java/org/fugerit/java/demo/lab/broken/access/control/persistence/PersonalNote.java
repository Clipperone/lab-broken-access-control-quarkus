package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fugerit.java.demo.lab.broken.access.control.dto.PersonalNoteResponseDTO;

import java.time.LocalDateTime;

/**
 * Entity per lo SCENARIO 1 (ownership): una nota personale, visibile solo all'owner o a un admin.
 *
 * <p>
 * Il campo {@code ownerUpn} è impostato lato server dall'identità autenticata (mai dal client) ed è la
 * base del controllo di autorizzazione per proprietà (ownership-based access control).
 * </p>
 */
@Entity
@Table(name = "PERSONAL_NOTE", schema = "LAB_BAC")
@Getter
@Setter
@ToString
public class PersonalNote extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "personalNoteSeqGenerator", sequenceName = "LAB_BAC.SEQ_PERSONAL_NOTE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "personalNoteSeqGenerator")
    @Column(name = "ID")
    private Long id;

    @Column(name = "UUID", length = 64)
    private String uuid;

    @Column(name = "OWNER_UPN", length = 128)
    private String ownerUpn;

    @Column(name = "TITLE", length = 512)
    private String title;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public PersonalNoteResponseDTO toDTO() {
        PersonalNoteResponseDTO dto = new PersonalNoteResponseDTO();
        dto.setUuid(this.uuid);
        dto.setOwnerUpn(this.ownerUpn);
        dto.setTitle(this.title);
        dto.setContent(this.content);
        dto.setCreationDate(this.creationDate);
        return dto;
    }

}
