/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

/**
 * The gas, water & energy for the EJB subsystem.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBUtilities implements Service<EJBUtilities> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "utilities");

    private final InjectedValue<SimpleSecurityManager> securityManagerValue = new InjectedValue<SimpleSecurityManager>();
    private final InjectedValue<TransactionManager> transactionManagerValue = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistryValue = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<UserTransaction> userTransactionValue = new InjectedValue<UserTransaction>();

    public SimpleSecurityManager getSecurityManager() {
        final SimpleSecurityManager securityManager = securityManagerValue.getOptionalValue();
        if (securityManager == null)
            throw new UnsupportedOperationException("Security is not enabled");
        return securityManager;
    }

    public Injector<SimpleSecurityManager> getSecurityManagerInjector() {
        return securityManagerValue;
    }

    public TransactionManager getTransactionManager() {
        return transactionManagerValue.getOptionalValue();
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return transactionManagerValue;
    }

    public InjectedValue<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
        return transactionSynchronizationRegistryValue;
    }

    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistryValue.getOptionalValue();
    }

    public UserTransaction getUserTransaction() {
        return userTransactionValue.getOptionalValue();
    }

    public Injector<UserTransaction> getUserTransactionInjector() {
        return userTransactionValue;
    }

    @Override
    public EJBUtilities getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public boolean hasSecurityManager() {
        return securityManagerValue.getOptionalValue() != null;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}
