/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Test that a peristence unit can be injected into a Hibernate session
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateSessionFactory {
    @PersistenceUnit(unitName = "mypc")
    SessionFactory sessionFactory;


    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        try {
            Session session = sessionFactory.openSession();
            session.persist(emp);
            session.flush();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting employee entity", e);
        }
    }

    public Employee getEmployee(int id) {
        Employee emp = sessionFactory.openSession().get(Employee.class, id);
        return emp;
    }


}
