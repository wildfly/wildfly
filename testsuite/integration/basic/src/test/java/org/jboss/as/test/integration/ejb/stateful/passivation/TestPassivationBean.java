/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation;

import java.util.Random;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remote;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Stateful
@Cache("distributable")
@Remote(TestPassivationRemote.class)
@Interceptors(PassivationInterceptor.class)
public class TestPassivationBean extends PassivationSuperClass implements TestPassivationRemote {
    private static final Logger log = Logger.getLogger(TestPassivationBean.class);

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @EJB
    private NestledBean nestledBean;

    @Inject
    private ManagedBean managedBean;

    private String identificator;
    private boolean beenPassivated = false;
    private boolean beenActivated = false;

    /**
     * Returns the expected result
     */
    @Override
    public String returnTrueString() {
        return TestPassivationRemote.EXPECTED_RESULT;
    }

    /**
     * Returns whether or not this instance has been passivated
     */
    @Override
    public boolean hasBeenPassivated() {
        return this.beenPassivated;
    }

    /**
     * Returns whether or not this instance has been activated
     */
    @Override
    public boolean hasBeenActivated() {
        return this.beenActivated;
    }

    @Override
    public boolean isPersistenceContextSame() {
        Employee e2 = nestledBean.get(1);
        Employee e1 = (Employee) entityManager.createQuery("select e from Employee e where e.id=:id").setParameter("id", 1)
                .getSingleResult();
        return e1 == e2;

    }

    @Override
    public void addEntity(final int id, final String name) {
        Employee e = new Employee();
        e.setName(name);
        e.setId(id);
        entityManager.persist(e);
        entityManager.flush();
    }

    @Override
    public void removeEntity(final int id) {
        Employee e = entityManager.find(Employee.class, id);
        entityManager.remove(e);
        entityManager.flush();
    }

    @Override
    public void setManagedBeanMessage(String message) {
        this.managedBean.setMessage(message);
    }

    @Override
    public String getManagedBeanMessage() {
        return managedBean.getMessage();
    }

    @PostConstruct
    public void postConstruct() {
        Random r = new Random();
        this.identificator = new Integer(r.nextInt(999)).toString();
        log.trace("Bean [" + this.identificator + "] created");
    }

    @PreDestroy
    public void preDestroy() {
        log.trace("Bean [" + this.identificator + "] destroyed");
    }

    @PrePassivate
    public void setPassivateFlag() {
        log.trace(this.toString() + " PrePassivation [" + this.identificator + "]");
        this.beenPassivated = true;
    }

    @PostActivate
    public void setActivateFlag() {
        log.trace(this.toString() + " PostActivation [" + this.identificator + "]");
        this.beenActivated = true;
    }

    @Remove
    @Override
    public void close() {
        log.trace("Bean [" + this.identificator + "] removing");
        this.nestledBean.close();
    }
}
