/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.jndi;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    public void createEmployee(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("createEmployee couldn't start tx", e);
        }
    }

    public Employee mutateEmployee(Employee employee) {

        employee.setName(employee.getName() + " +1");

        // load the entity with no Jakarta Transactions transaction (entity should be detached from persistence context)
        getEmployeeNoTX(employee.getId());

        // update the entity, which will increment the version field
        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.merge(employee);
            em.flush();
            tx1.commit();

            // load the entity with no Jakarta Transactions transaction (should get fresh copy of entity with updated version field)
            return getEmployeeNoTX(employee.getId());
        } catch (Exception e) {
            throw new RuntimeException("mutateEmployee couldn't start tx", e);
        }
    }

    public Employee getEmployeeNoTX(int id) {
        return em.find(Employee.class, id);
    }
}
