/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import jakarta.ejb.Stateful;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.hibernate.Session;

/**
 * Test that a peristence context can be injected into a Hibernate session
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSBHibernateSession {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    Session session;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        session.persist(emp);
    }

    public Employee getEmployee(int id) {
        Employee emp = session.get(Employee.class, id);
        return emp;
    }


}
