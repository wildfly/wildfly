/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.initializeinorder;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * @author Scott Marlow
 */
public abstract class AbstractCMTBean {
    @PersistenceContext(unitName = "pu1")
    EntityManager em;

    @PersistenceContext(unitName = "pu2")
    EntityManager em2;


    @Resource
    SessionContext sessionContext;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.joinTransaction();
        em.persist(emp);
        //em.flush();
    }

    public void updateEmployee(Employee emp) {
        emp.setName("hacked " + emp.getName());
        em2.merge(emp);
        //em.flush();
    }


    public Employee getEmployeeNoTX(int id) {
        return em.find(Employee.class, id, LockModeType.NONE);
    }
}
