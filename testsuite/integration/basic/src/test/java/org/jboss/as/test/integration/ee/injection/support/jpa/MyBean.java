/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MyBean {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @PersistenceContext(unitName = "onephasePU")
    EntityManager onephaseEm;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.joinTransaction();
        em.persist(emp);
    }

    public Employee getEmployeeById(int id) {
        return onephaseEm.find(Employee.class, id, LockModeType.NONE);
    }
}
