/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.beanmanager;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
@LocalBean
public class TestBean {

    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.joinTransaction();
        em.persist(emp);
    }

    public void updateEmployee(int id) {
        Employee employee = em.find(Employee.class, id);
        employee.setName("Johny");
        em.merge(employee);
    }

    public void removeEmployee(int id) {
        Employee employee = em.find(Employee.class, id);
        em.remove(employee);
    }


}


