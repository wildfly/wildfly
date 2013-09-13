/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.jboss.tm.TxUtils;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB1 {
    @PersistenceContext
    EntityManager entityManager;

    static private boolean afterCompletionCalledByTMTimeoutThread = false;

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
    public void createEmployeeWaitForTxTimeout(boolean registerTxSync, String name, String address, int id) {
        System.out.println("org.jboss.as.test.integration.jpa.mockprovider.txtimeout.createEmployeeWaitForTxTimeout " +
                "entered, will wait for tx time out to occur");
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        entityManager.persist(emp);
        boolean done = false;
        if (registerTxSync) {
            transactionSynchronizationRegistry.registerInterposedSynchronization(
                    new Synchronization() {
                        @Override
                        public void beforeCompletion() {

                        }

                        @Override
                        public void afterCompletion(int status) {
                            afterCompletionCalledByTMTimeoutThread =
                                TxUtils.isTransactionManagerTimeoutThread();
                        }
                    });
        }

        while(!done) {
            try {
                Thread.sleep(250);
                entityManager.find(Employee.class, id);
                int status = transactionSynchronizationRegistry.getTransactionStatus();
                switch(status) {
                    case Status.STATUS_COMMITTED:
                        throw new RuntimeException("transaction was committed.");
                    case Status.STATUS_ROLLEDBACK:
                        System.out.println("tx timed out and rolled back as expected, success case reached.");
                        done = true;
                        break;
                    case Status.STATUS_ACTIVE:
                        System.out.println("tx is still active, sleep for 250ms and check tx status again.");
                        break;
                    default:
                        System.out.println("tx status = " + status +", sleep for 250ms and check tx status again.");
                        break;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            System.out.println("org.jboss.as.test.integration.jpa.mockprovider.txtimeout.createEmployeeWaitForTxTimeout waiting for tx to timeout");
        }

    }

    public Employee getEmployeeNoTX(int id) {
        return entityManager.find(Employee.class, id, LockModeType.NONE);
    }

    /**
     * @return true if the afterCompletion synchronization used in
     * createEmployeeWaitForTxTimeout was called by the transaction
     * manager handling a transaction timeout, otherwise return false
     */
    public static boolean isAfterCompletionCalledByTMTimeoutThread() {
        return afterCompletionCalledByTMTimeoutThread;
    }
}
