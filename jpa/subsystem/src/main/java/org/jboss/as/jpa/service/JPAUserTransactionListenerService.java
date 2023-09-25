/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.service;

import org.jboss.as.jpa.container.JPAUserTransactionListener;
import org.jboss.as.txn.service.UserTransactionRegistryService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * listen for user transaction begin events
 *
 * @author Scott Marlow
 */
public class JPAUserTransactionListenerService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jpa-usertransactionlistener");

    private final InjectedValue<UserTransactionRegistry> userTransactionRegistryInjectedValue = new InjectedValue<UserTransactionRegistry>();
    private volatile JPAUserTransactionListener jpaUserTransactionListener = null;

    @Override
    public void start(StartContext context) throws StartException {
        jpaUserTransactionListener = new JPAUserTransactionListener();
        userTransactionRegistryInjectedValue.getValue().addListener(jpaUserTransactionListener);
    }

    @Override
    public void stop(StopContext context) {
        userTransactionRegistryInjectedValue.getValue().removeListener(jpaUserTransactionListener);
        jpaUserTransactionListener = null;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public InjectedValue<UserTransactionRegistry> getUserTransactionRegistryInjectedValue() {
        return userTransactionRegistryInjectedValue;
    }

    public static void addService(ServiceTarget target) {

        JPAUserTransactionListenerService jpaUserTransactionListenerService = new JPAUserTransactionListenerService();

        target.addService(SERVICE_NAME, jpaUserTransactionListenerService)
                .addDependency(UserTransactionRegistryService.SERVICE_NAME, UserTransactionRegistry.class, jpaUserTransactionListenerService.getUserTransactionRegistryInjectedValue())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
