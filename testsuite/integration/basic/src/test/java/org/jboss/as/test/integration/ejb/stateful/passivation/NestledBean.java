package org.jboss.as.test.integration.ejb.stateful.passivation;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author Stuart Douglas
 */
@Stateful
@Cache("distributable")
public class NestledBean {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public Employee get(int id) {
        return (Employee) entityManager.createQuery("select e from Employee e where e.id=:id").setParameter("id", id)
                .getSingleResult();
    }

    @Remove
    public void close() {
    }
}
