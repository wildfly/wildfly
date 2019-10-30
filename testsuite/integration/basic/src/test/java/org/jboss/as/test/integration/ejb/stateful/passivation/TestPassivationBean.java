/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.stateful.passivation;

import java.util.Random;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Stateful
@Cache("passivating")
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
