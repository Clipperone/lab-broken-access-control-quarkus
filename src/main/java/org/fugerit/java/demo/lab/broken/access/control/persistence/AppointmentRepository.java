package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Repository per la entity {@link Appointment}.
 */
@ApplicationScoped
public class AppointmentRepository implements PanacheRepository<Appointment> {

    public Appointment findByUuid(String uuid) {
        return find("uuid", uuid).firstResult();
    }

    public List<Appointment> findAllOrdered() {
        return find("order by appointmentAt").list();
    }

    /**
     * Verifica se esiste già un appuntamento per lo stesso scienziato nello stesso slot temporale (stesso secondo),
     * escludendo eventualmente un appuntamento (per lo spostamento, dove non deve contare sé stesso).
     * Il confronto sulla finestra [start, start+1s) è robusto alle differenze di precisione sub-secondo tra il
     * valore persistito e il parametro.
     */
    public boolean hasConflict(String scientistUpn, LocalDateTime at, String excludeUuid) {
        LocalDateTime start = at.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = start.plusSeconds(1);
        if (excludeUuid == null || excludeUuid.isEmpty()) {
            return count("scientistUpn = ?1 and appointmentAt >= ?2 and appointmentAt < ?3", scientistUpn, start, end) > 0;
        }
        return count("scientistUpn = ?1 and appointmentAt >= ?2 and appointmentAt < ?3 and uuid <> ?4", scientistUpn, start,
                end, excludeUuid) > 0;
    }

}
