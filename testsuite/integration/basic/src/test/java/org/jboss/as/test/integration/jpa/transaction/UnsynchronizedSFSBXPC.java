/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.SynchronizationType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class UnsynchronizedSFSBXPC {
    @PersistenceContext(unitName = "unsynchronized", type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
    EntityManager em;

    public Employee createAndFind(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return em.find(Employee.class, id);
    }

    public Employee find(int id) {
        return em.find(Employee.class, id);
    }

}
