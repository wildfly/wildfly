/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.wildfly.common.Assert;

/**
 * WildFlyCustomJtaPlatform can obtain the Jakarta Transactions TransactionSynchronizationRegistry to be used by
 * Hibernate ORM Jakarta Persistence + native applications.
 * For Jakarta Persistence applications, we could of passed the TransactionSynchronizationRegistry into the
 * constructor but Hibernate native apps wouldn't be able to do that, so this covers all app types.
 *
 * @author Scott Marlow
 */
public class WildFlyCustomJtaPlatform extends JBossAppServerJtaPlatform implements JtaSynchronizationStrategy {

    // The 'transactionSynchronizationRegistry' used by Jakarta Persistence container managed applications,
    // is reset every time the Transaction Manager service is restarted,
    // as (application deployment) Jakarta Persistence persistence unit service depends on the TM service.
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
     * services are stopped but Jakarta Persistence container managed applications can and will call setTransactionSynchronizationRegistry
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
