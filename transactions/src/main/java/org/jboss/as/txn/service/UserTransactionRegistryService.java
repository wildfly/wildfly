/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * Service responsible for exposing a {@link UserTransactionRegistry} instance.
 *
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionRegistryService {
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY;

    public static void addService(final ServiceTarget target) {
        final ServiceBuilder<?> sb = target.addService();
        final Consumer<UserTransactionRegistry> userTxnRegistryConsumer = sb.provides(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY);
        sb.setInstance(Service.newInstance(userTxnRegistryConsumer, new UserTransactionRegistry()));
        sb.install();
    }
}
