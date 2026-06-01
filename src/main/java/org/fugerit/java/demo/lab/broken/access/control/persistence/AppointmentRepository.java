package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

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

}
