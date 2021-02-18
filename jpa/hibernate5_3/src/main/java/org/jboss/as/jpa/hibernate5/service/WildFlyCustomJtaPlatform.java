/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate5.service;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.wildfly.common.Assert;

/**
 * WildFlyCustomJtaPlatform can obtain the Jakarta Transactions TransactionSynchronizationRegistry to be used by
 * Hibernate ORM JPA + native applications.
 * For JPA applications, we could of passed the TransactionSynchronizationRegistry into the
 * constructor but Hibernate native apps wouldn't be able to do that, so this covers all app types.
 *
 * @author Scott Marlow
 */
public class WildFlyCustomJtaPlatform extends JBossAppServerJtaPlatform implements JtaSynchronizationStrategy {

    // The 'transactionSynchronizationRegistry' used by JPA container managed applications,
    // is reset every time the Transaction Manager service is restarted,
    // as (application deployment) JPA persistence unit service depends on the TM service.
    // For this reason, the static 'transactionSynchronizationRegistry' can be updated.
    // Note that Hibernate native applications currently have to be (manually) restarted when the TM
    // service restarts, as native applications do not have WildFly service dependencies set for them.
    private static volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static final String TSR_NAME = "java:jboss/TransactionSynchronizationRegistry";

    // JtaSynchronizationStrategy
    @Override
    public void registerSynchronization(Synchronization synchronization) {
        locateTransactionSynchronizationRegistry().
                registerInterposedSynchronization(synchronization);
    }

    // JtaSynchronizationStrategy
    @Override
    public boolean canRegisterSynchronization() {
        return locateTransactionSynchronizationRegistry().
                getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    @Override
    protected JtaSynchronizationStrategy getSynchronizationStrategy() {
        return this;
    }

    private TransactionSynchronizationRegistry locateTransactionSynchronizationRegistry() {
        TransactionSynchronizationRegistry curTsr = transactionSynchronizationRegistry;
        if (curTsr != null) {
            return curTsr;
        }
        synchronized (WildFlyCustomJtaPlatform.class) {
            curTsr = transactionSynchronizationRegistry;
            if (curTsr != null) {
                return curTsr;
            }
            return transactionSynchronizationRegistry = (TransactionSynchronizationRegistry) jndiService().locate(TSR_NAME);
        }
    }

    /**
     * Hibernate native applications cannot know when the TransactionManaTransactionManagerSerger + TransactionSynchronizationRegistry
     * services are stopped but JPA container managed applications can and will call setTransactionSynchronizationRegistry
     * with the new (global) TransactionSynchronizationRegistry to use.
     *
     * @param tsr
     */
    public static void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry tsr) {

        if ((Assert.checkNotNullParam("tsr", tsr)) != transactionSynchronizationRegistry) {
            synchronized (WildFlyCustomJtaPlatform.class) {
                if (tsr != transactionSynchronizationRegistry) {
                    transactionSynchronizationRegistry = tsr;
                }
            }
        }
    }

}
