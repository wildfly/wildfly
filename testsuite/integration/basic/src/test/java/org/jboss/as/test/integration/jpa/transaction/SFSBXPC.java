/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.transaction;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
     * the container associates the persistence context with the JTA transaction and calls
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
