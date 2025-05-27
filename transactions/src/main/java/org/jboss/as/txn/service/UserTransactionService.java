/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;

import jakarta.transaction.UserTransaction;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.transaction.client.LocalUserTransaction;

/**
 * Service responsible for getting the {@link UserTransaction}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionService {
    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.LocalTransactionContext to obtain a UserTransaction reference. */
    @Deprecated
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;
    /** Non-deprecated service name only for use within the subsystem */
    @SuppressWarnings("deprecation")
    public static final ServiceName INTERNAL_SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION;

    public static void addService(final ServiceTarget target) {
        final ServiceBuilder<?> sb = target.addService();
        final Consumer<UserTransaction> userTransactionConsumer = sb.provides(INTERNAL_SERVICE_NAME);
        sb.setInstance(Service.newInstance(userTransactionConsumer, LocalUserTransaction.getInstance()));
        sb.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        sb.install();
    }
}
