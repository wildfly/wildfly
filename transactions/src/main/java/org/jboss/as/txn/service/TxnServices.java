/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 */
public final class TxnServices {

    public static final ServiceName JBOSS_TXN = ServiceName.JBOSS.append("txn");

    public static final ServiceName JBOSS_TXN_PATHS = JBOSS_TXN.append("paths");

    public static final ServiceName JBOSS_TXN_CORE_ENVIRONMENT = JBOSS_TXN.append("CoreEnvironment");

    public static final ServiceName JBOSS_TXN_XA_TERMINATOR = JBOSS_TXN.append("XATerminator");

    public static final ServiceName JBOSS_TXN_EXTENDED_JBOSS_XA_TERMINATOR = JBOSS_TXN.append("ExtendedJBossXATerminator");

    public static final ServiceName JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT = JBOSS_TXN.append("ArjunaObjectStoreEnvironment");

    public static final ServiceName JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER = JBOSS_TXN.append("ArjunaTransactionManager");

    /** @deprecated Use the "org.wildfly.transactions.xa-resource-recovery-registry" capability */
    @Deprecated
    public static final ServiceName JBOSS_TXN_ARJUNA_RECOVERY_MANAGER = JBOSS_TXN.append("ArjunaRecoveryManager");

    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.ContextTransactionManager to obtain a TransactionManager reference. */
    @Deprecated
    public static final ServiceName JBOSS_TXN_TRANSACTION_MANAGER = JBOSS_TXN.append("TransactionManager");

    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.LocalTransactionContext to obtain a UserTransaction reference. */
    @Deprecated
    public static final ServiceName JBOSS_TXN_USER_TRANSACTION = JBOSS_TXN.append("UserTransaction");

    public static final ServiceName JBOSS_TXN_USER_TRANSACTION_REGISTRY = JBOSS_TXN.append("UserTransactionRegistry");

    /** @deprecated Use the "org.wildfly.transactions.transaction-synchronization-registry" capability */
    @Deprecated
    public static final ServiceName JBOSS_TXN_SYNCHRONIZATION_REGISTRY = JBOSS_TXN.append("TransactionSynchronizationRegistry");

    public static final ServiceName JBOSS_TXN_JTA_ENVIRONMENT = JBOSS_TXN.append("JTAEnvironment");

    public static final ServiceName JBOSS_TXN_CMR = JBOSS_TXN.append("CMR");

    public static final ServiceName JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT = JBOSS_TXN.append("context", "local");

    public static final ServiceName JBOSS_TXN_REMOTE_TRANSACTION_SERVICE = JBOSS_TXN.append("service", "remote");

    public static final ServiceName JBOSS_TXN_HTTP_REMOTE_TRANSACTION_SERVICE = JBOSS_TXN.append("service", "http-remote");

    public static final ServiceName JBOSS_TXN_CONTEXT_XA_TERMINATOR = JBOSS_TXN.append("JBossContextXATerminator");


    public static <T> T notNull(T value) {
        if (value == null) throw TransactionLogger.ROOT_LOGGER.serviceNotStarted();
        return value;
    }

    private TxnServices() {
    }
}
