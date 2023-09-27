/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {
    private static final Logger LOGGER = Logger.getLogger(SFSB1.class);
    @PersistenceContext
    EntityManager entityManager;

    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        entityManager.persist(emp);
    }

    @TransactionTimeout(value = 1, unit = TimeUnit.SECONDS)
    public void createEmployeeWaitForTxTimeout(String name, String address, int id) {
        LOGGER.trace("org.jboss.as.test.integration.jpa.mockprovider.txtimeout.createEmployeeWaitForTxTimeout " +
                "entered, will wait for tx time out to occur");
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        entityManager.persist(emp);
        boolean done = false;

        while (!done) {
            try {
                Thread.sleep(250);
                entityManager.find(Employee.class, id);
                int status = transactionSynchronizationRegistry.getTransactionStatus();
                switch (status) {
                    case Status.STATUS_COMMITTED:
                        throw new RuntimeException("transaction was committed.");
                    case Status.STATUS_ROLLEDBACK:
                        LOGGER.trace("tx timed out and rolled back as expected, success case reached.");
                        done = true;
                        break;
                    case Status.STATUS_ACTIVE:
                        LOGGER.trace("tx is still active, sleep for 250ms and check tx status again.");
                        break;
                    default:
                        LOGGER.trace("tx status = " + status + ", sleep for 250ms and check tx status again.");
                        break;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            LOGGER.trace("org.jboss.as.test.integration.jpa.mockprovider.txtimeout.createEmployeeWaitForTxTimeout waiting for tx to timeout");
        }
    }

    public Employee getEmployeeNoTX(int id) {
        return entityManager.find(Employee.class, id, LockModeType.NONE);
    }

}
