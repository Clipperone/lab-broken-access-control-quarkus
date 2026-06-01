package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fugerit.java.demo.lab.broken.access.control.dto.AppointmentResponseDTO;

import java.time.LocalDateTime;

/**
 * Entità per lo scenario APPUNTAMENTI. Un appuntamento collega più soggetti:
 * <ul>
 * <li>{@code creatorUpn} — chi lo ha prenotato (impostato dal token, mai dal client);</li>
 * <li>{@code scientistUpn} — lo scienziato destinatario;</li>
 * <li>{@code office} — l'ufficio dell'appuntamento (il cui admin potrà vederlo).</li>
 * </ul>
 *
 * <p>
 * Introduce l'autorizzazione <b>basata sul tempo</b>: l'eliminazione dipende da {@code appointmentAt}
 * rispetto all'ora corrente (consentita solo se mancano più di 24h).
 * </p>
 */
@Entity
@Table(name = "APPOINTMENT", schema = "LAB_BAC")
@Getter
@Setter
@ToString
public class Appointment extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "appointmentSeqGenerator", sequenceName = "LAB_BAC.SEQ_APPOINTMENT", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointmentSeqGenerator")
    @Column(name = "ID")
    private Long id;

    @Column(name = "UUID", length = 64)
    private String uuid;

    @Column(name = "CREATOR_UPN", length = 128)
    private String creatorUpn;

    @Column(name = "SCIENTIST_UPN", length = 128)
    private String scientistUpn;

    @Column(name = "OFFICE", length = 128)
    private String office;

    @Column(name = "APPOINTMENT_AT")
    private LocalDateTime appointmentAt;

    @Column(name = "SUBJECT", length = 512)
    private String subject;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    public AppointmentResponseDTO toDTO() {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setUuid(this.uuid);
        dto.setCreatorUpn(this.creatorUpn);
        dto.setScientistUpn(this.scientistUpn);
        dto.setOffice(this.office);
        dto.setAppointmentAt(this.appointmentAt);
        dto.setSubject(this.subject);
        dto.setCreationDate(this.creationDate);
        return dto;
    }

}
