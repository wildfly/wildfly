/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import jakarta.transaction.UserTransaction;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.transaction.client.LocalUserTransaction;

/**
 * Service responsible for getting the {@link UserTransaction}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class UserTransactionService implements Service<UserTransaction> {
    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.LocalTransactionContext to obtain a UserTransaction reference. */
    @Deprecated
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;
    /** Non-deprecated service name only for use within the subsystem */
    @SuppressWarnings("deprecation")
    public static final ServiceName INTERNAL_SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;

    private static final UserTransactionService INSTANCE = new UserTransactionService();

    private UserTransactionService() {
    }

    public static ServiceController<UserTransaction> addService(final ServiceTarget target) {
        ServiceBuilder<UserTransaction> serviceBuilder = target.addService(INTERNAL_SERVICE_NAME, INSTANCE);
        serviceBuilder.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        return serviceBuilder.install();
    }

    @Override
    public void start(final StartContext startContext) {
        // noop
    }

    @Override
    public void stop(final StopContext stopContext) {
        // noop
    }

    @Override
    public UserTransaction getValue() throws IllegalStateException {
        return LocalUserTransaction.getInstance();
    }
}
