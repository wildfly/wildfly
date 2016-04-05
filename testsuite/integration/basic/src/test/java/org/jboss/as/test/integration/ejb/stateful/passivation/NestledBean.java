package org.jboss.as.test.integration.ejb.stateful.passivation;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author Stuart Douglas
 */
@Stateful
@Cache("passivating")
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
