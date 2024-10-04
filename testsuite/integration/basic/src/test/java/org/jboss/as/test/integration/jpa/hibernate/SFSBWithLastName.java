/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSBWithLastName {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public void createEmployee(String name, String address, String id) {
        EmployeeWithLastName emp = new EmployeeWithLastName();
        emp.setId(id);
        emp.setAddress(address);
        emp.setLastName(name);
        em.persist(emp);
    }

    public EmployeeWithLastName getEmployeeNoTX(String id) {
        return em.find(EmployeeWithLastName.class, id, LockModeType.NONE);
    }

}
