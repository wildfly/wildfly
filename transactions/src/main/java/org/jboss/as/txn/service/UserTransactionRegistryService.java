/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * Service responsible for exposing a {@link UserTransactionRegistry} instance.
 *
 * @author John Bailey
 */
public class UserTransactionRegistryService implements Service<UserTransactionRegistry> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY;

    private UserTransactionRegistry userTransactionRegistry;

    public synchronized void start(StartContext context) throws StartException {
        userTransactionRegistry = new UserTransactionRegistry();
    }

    public synchronized void stop(StopContext context) {
        userTransactionRegistry = null;
    }

    public synchronized UserTransactionRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return userTransactionRegistry;
    }
}
