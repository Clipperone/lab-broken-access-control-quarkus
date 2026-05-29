package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository per la entity {@link OfficeDocument}.
 */
@ApplicationScoped
public class OfficeDocumentRepository implements PanacheRepository<OfficeDocument> {

    public OfficeDocument findByUuid(String uuid) {
        return find("uuid", uuid).firstResult();
    }

    public List<OfficeDocument> findAllOrdered() {
        return find("order by creationDate").list();
    }

}
