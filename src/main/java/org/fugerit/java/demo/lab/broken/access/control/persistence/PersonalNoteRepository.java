package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository per la entity {@link PersonalNote}.
 */
@ApplicationScoped
public class PersonalNoteRepository implements PanacheRepository<PersonalNote> {

    public PersonalNote findByUuid(String uuid) {
        return find("uuid", uuid).firstResult();
    }

    /** Note di proprietà dell'owner indicato, ordinate per data di creazione. */
    public List<PersonalNote> findByOwner(String ownerUpn) {
        return find("ownerUpn = ?1 order by creationDate", ownerUpn).list();
    }

    /** Tutte le note (uso riservato all'admin), ordinate per data di creazione. */
    public List<PersonalNote> findAllOrdered() {
        return find("order by creationDate").list();
    }

}
