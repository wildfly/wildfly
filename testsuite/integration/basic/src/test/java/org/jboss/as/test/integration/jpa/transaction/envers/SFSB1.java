/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction.envers;

import static org.junit.Assert.assertEquals;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * stateful session bean
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    // create Employee
    public Employee createEmployeeTx(String name, String address, int id) {

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
            return emp;
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);

        }

    }

    public void updateEmployeeTx(String address, Employee emp) {

        emp.setAddress(address);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            //System.out.println("Inside JTA updateEmployeeTx after transaction begins:--");
            em.joinTransaction();
            em.merge(emp);
            tx1.commit();

        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }

    }

    public void updateEmployeeTxwithRollBack(String address, Employee emp) {

        emp.setAddress(address);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.merge(emp);
            em.getTransaction().setRollbackOnly(); // force rollback of transaction
            tx1.commit();
        } catch (Exception e) {

            throw new RuntimeException("couldn't start tx", e);
        } finally {
            String obtainedaddress = retrieveOldEmployeeVersionforRollBack(emp.getId());
            assertEquals("Red Hat Purkynova Brno", obtainedaddress);

        }

    }

    public String retrieveOldEmployeeVersion(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Employee emp1_rev = reader.find(Employee.class, id, 1);
        return emp1_rev.getAddress();
    }

    public String retrieveOldEmployeeVersionforRollBack(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Employee emp1_rev = reader.find(Employee.class, id, 5);
        return emp1_rev.getAddress();
    }

}
