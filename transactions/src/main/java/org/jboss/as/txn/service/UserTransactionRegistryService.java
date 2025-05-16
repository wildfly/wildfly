/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.usertx.UserTransactionRegistry;

import java.util.function.Consumer;

/**
 * Service responsible for exposing a {@link UserTransactionRegistry} instance.
 *
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionRegistryService implements Service {
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY;
    private final Consumer<UserTransactionRegistry> userTxnRegistryConsumer;

    private UserTransactionRegistryService(final Consumer<UserTransactionRegistry> userTxnRegistryConsumer) {
        this.userTxnRegistryConsumer = userTxnRegistryConsumer;
    }

    public void start(final StartContext context) throws StartException {
        userTxnRegistryConsumer.accept(new UserTransactionRegistry());
    }

    public void stop(final StopContext context) {
        userTxnRegistryConsumer.accept(null);
    }

    public static ServiceController<?> addService(final ServiceTarget target) {
        final ServiceBuilder<?> userTxnServiceSB = target.addService();
        final Consumer<UserTransactionRegistry> userTxnRegistryConsumer = userTxnServiceSB.provides(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY);
        return userTxnServiceSB.setInstance(new UserTransactionRegistryService(userTxnRegistryConsumer)).install();
    }
}
