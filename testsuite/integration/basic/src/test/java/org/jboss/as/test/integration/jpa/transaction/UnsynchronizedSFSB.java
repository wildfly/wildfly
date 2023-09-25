/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SynchronizationType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class UnsynchronizedSFSB {
    @PersistenceContext(unitName = "unsynchronized", synchronization = SynchronizationType.UNSYNCHRONIZED)
    EntityManager em;

    @EJB
    InnerUnsynchronizedSFSB innerUnsynchronizedSFSB;    // for UNSYNCHRONIZED propagation test

    @EJB
    InnerSynchronizedSFSB innerSynchronizedSFSB;        // forces a mixed SYNCHRONIZATION/UNSYNCHRONIZED ERROR


    public Employee createAndFind(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return em.find(Employee.class, id);
    }

    public void createAndJoin(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
        em.joinTransaction();   // new employee will be saved to database
    }

    public Employee find(int id) {
        return em.find(Employee.class, id);
    }

    public Employee createAndPropagatedFind(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return innerUnsynchronizedSFSB.find(id);
    }

    public Employee createAndPropagatedFindMixExceptionExcepted(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return innerSynchronizedSFSB.find(id);
    }

    public void createAndPropagatedJoin(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        innerUnsynchronizedSFSB.joinTransaction();
    }

}
