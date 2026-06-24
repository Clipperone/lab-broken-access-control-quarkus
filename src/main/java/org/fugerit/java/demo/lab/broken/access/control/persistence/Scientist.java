package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fugerit.java.demo.lab.broken.access.control.dto.ScientistResponseDTO;

/**
 * Registro degli scienziati: mappa l'{@code upn} di uno scienziato al suo {@code office}. È la fonte di
 * verità lato server per derivare l'ufficio di un appuntamento (mai dal client → niente mass assignment
 * né salti di tenant).
 */
@Entity
@Table(name = "SCIENTIST", schema = "LAB_BAC")
@Getter
@Setter
@ToString
public class Scientist extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "scientistSeqGenerator", sequenceName = "LAB_BAC.SEQ_SCIENTIST", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scientistSeqGenerator")
    @Column(name = "ID")
    private Long id;

    @Column(name = "UPN", length = 128)
    private String upn;

    @Column(name = "OFFICE", length = 128)
    private String office;

    @Column(name = "DISPLAY_NAME", length = 256)
    private String displayName;

    public ScientistResponseDTO toDTO() {
        ScientistResponseDTO dto = new ScientistResponseDTO();
        dto.setUpn(this.upn);
        dto.setOffice(this.office);
        dto.setDisplayName(this.displayName);
        return dto;
    }

}
