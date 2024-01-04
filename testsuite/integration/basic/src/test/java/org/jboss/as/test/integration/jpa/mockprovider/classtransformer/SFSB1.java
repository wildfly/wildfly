/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceUnit;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {
    @PersistenceUnit
    EntityManagerFactory emf;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        emf.createEntityManager().persist(emp);
    }

    public Employee getEmployeeNoTX(int id) {
        return emf.createEntityManager().find(Employee.class, id, LockModeType.NONE);
    }

}
