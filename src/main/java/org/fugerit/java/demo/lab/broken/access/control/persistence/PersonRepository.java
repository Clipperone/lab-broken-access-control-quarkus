package org.fugerit.java.demo.lab.broken.access.control.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;

/**
 * Repository per la entity Person
 */
@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {

    /**
     * Cerca una persona tramite il campo UUID.
     *
     * @param uuid l'UUID della persona da cercare
     * @return la {@link Person} corrispondente all'UUID fornito, o {@code null} se non trovata
     */
    public Person findByUuid(String uuid) {
        return find("uuid", uuid).firstResult();
    }

    /**
     * Restituisce l'elenco delle persone ordinate per cognome e nome,
     * filtrate per MIN_ROLE (NULL oppure presente nella collection di ruoli fornita)
     *
     * @param roles Collection dei ruoli dell'utente
     * @return Lista di Person filtrate e ordinate
     */
    public List<Person> findByRolesOrderedByName(Collection<String> roles) {
        // VULNERABILITY: (2) l'elenco dei ruoli è tra i parametri del metodo ma NON viene usato come filtro:
        // la query restituisce TUTTE le persone, anche quelle con minRole superiore al ruolo dell'utente
        // (escalation orizzontale dei dati). La soluzione è usare 'roles' nel filtro.
        return find("order by lastName, firstName").list();
    }

}