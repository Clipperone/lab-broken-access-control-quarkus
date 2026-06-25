package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository per il registro {@link Scientist}.
 */
@ApplicationScoped
public class ScientistRepository implements PanacheRepository<Scientist> {

    public Scientist findByUpn(String upn) {
        return find("upn", upn).firstResult();
    }

    public List<Scientist> findAllOrdered() {
        return find("order by upn").list();
    }

}
