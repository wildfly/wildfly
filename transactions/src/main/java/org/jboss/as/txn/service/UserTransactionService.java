/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import jakarta.transaction.UserTransaction;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.transaction.client.LocalUserTransaction;

import java.util.function.Consumer;

/**
 * Service responsible for getting the {@link UserTransaction}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionService implements Service {
    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.LocalTransactionContext to obtain a UserTransaction reference. */
    @Deprecated
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;
    /** Non-deprecated service name only for use within the subsystem */
    @SuppressWarnings("deprecation")
    public static final ServiceName INTERNAL_SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;
    private final Consumer<UserTransaction> userTransactionConsumer;

    private UserTransactionService(final Consumer<UserTransaction> userTransactionConsumer) {
        this.userTransactionConsumer = userTransactionConsumer;
    }

    @Override
    public void start(final StartContext startContext) {
        userTransactionConsumer.accept(LocalUserTransaction.getInstance());
    }

    @Override
    public void stop(final StopContext stopContext) {
        userTransactionConsumer.accept(null);
    }

    public static ServiceController<?> addService(final ServiceTarget target) {
        final ServiceBuilder<?> sb = target.addService();
        final Consumer<UserTransaction> userTransactionConsumer = sb.provides(INTERNAL_SERVICE_NAME);
        sb.setInstance(new UserTransactionService(userTransactionConsumer));
        sb.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        return sb.install();
    }
}
