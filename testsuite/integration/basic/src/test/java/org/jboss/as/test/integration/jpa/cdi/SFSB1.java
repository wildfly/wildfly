/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.cdi;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {

    @Inject
    @Pu1Qualifier
    EntityManager em;


    public void createEmployee(String name, String address, int id) {


        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Employee getEmployeeNoTX(int id) {

        return em.find(Employee.class, id);
    }

}
