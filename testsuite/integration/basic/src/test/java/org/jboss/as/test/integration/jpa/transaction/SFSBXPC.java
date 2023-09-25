/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * stateful session bean with an extended persistence context
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBXPC {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager extendedEm;

    @Resource
    UserTransaction tx;

    public void createEmployeeNoTx(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        extendedEm.persist(emp);
    }

    /**
     * UT part of test for JPA 7.9.1 Container Responsibilities for XPC
     * <p>
     * "When a business method of the stateful session bean is invoked, if the stateful session bean
     * uses bean managed transaction demarcation and a UserTransaction is begun within the method,
     * the container associates the persistence context with the Jakarta Transactions transaction and calls
     * EntityManager.joinTransaction.
     * "
     */
    public void savePendingChanges() {

        try {
            tx.begin();
            tx.commit();
        } catch (Exception e) {
            throw new RuntimeException("could not commit pending extended persistence changes", e);
        }
    }


    public void forceRollbackAndLosePendingChanges(int id, boolean shouldOfSavedAlready) {
        Employee employee = extendedEm.find(Employee.class, id);
        if (employee == null) { // employee should be found
            throw new RuntimeException("pending database changes were not saved previously, could not find Employee id = " + id);
        }
        try {
            tx.begin();
            tx.rollback();
        } catch (NotSupportedException ignore) {

        } catch (SystemException ignore) {

        }

        employee = extendedEm.find(Employee.class, id);
        if (shouldOfSavedAlready && employee == null) {
            throw new RuntimeException("unexpectedly in forceRollbackAndLosePendingChanges(), rollback lost Employee id = " + id + ", which should of been previously saved");
        } else if (!shouldOfSavedAlready && employee != null) {
            throw new RuntimeException("unexpectedly in forceRollbackAndLosePendingChanges(), database changes shouldn't of been saved yet for Employee id = " + id);
        }


    }

    public Employee lookup(int empid) {
        return extendedEm.find(Employee.class, empid);
    }

}
