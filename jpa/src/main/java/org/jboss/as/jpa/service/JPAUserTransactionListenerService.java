/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.service;

import org.jboss.as.jpa.container.JPAUserTransactionListener;
import org.jboss.as.txn.service.UserTransactionRegistryService;
import org.jboss.msc.inject.CastingInjector;
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
                .addDependency(UserTransactionRegistryService.SERVICE_NAME, new CastingInjector<UserTransactionRegistry>(jpaUserTransactionListenerService.getUserTransactionRegistryInjectedValue(), UserTransactionRegistry.class))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
