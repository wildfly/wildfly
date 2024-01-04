/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.resourcelocal;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceUnit;

/**
 * stateful session bean
 *
 * @author Stuart Douglas
 */
@Stateful
public class SFSB1 {
    @PersistenceUnit(unitName = "mypc")
    private EntityManagerFactory emf;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void createEmployeeNoJTATransaction(String name, String address, int id) {
        EntityManager em = emf.createEntityManager();
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        EntityTransaction tx1 = em.getTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }
    }

    public void flushWithNoTx() {
        EntityManager em = emf.createEntityManager();
        em.flush();// should throw TransactionRequiredException
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Employee getEmployeeNoTX(int id) {
        EntityManager em = emf.createEntityManager();

        return em.find(Employee.class, id);
    }

}
