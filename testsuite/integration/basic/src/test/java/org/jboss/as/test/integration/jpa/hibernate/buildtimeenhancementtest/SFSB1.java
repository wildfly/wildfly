/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
    }

    public Employee getEmployeeNoTX(int id) {
        return em.find(Employee.class, id, LockModeType.NONE);
    }

}
