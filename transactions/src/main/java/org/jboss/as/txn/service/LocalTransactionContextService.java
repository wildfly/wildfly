/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.service;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.ExtendedJBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.jboss.JBossLocalTransactionProvider;

/**
 * The service which provides the {@link LocalTransactionContext} for the server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalTransactionContextService implements Service<LocalTransactionContext> {
    private volatile LocalTransactionContext context;
    private final InjectedValue<ExtendedJBossXATerminator> extendedJBossXATerminatorInjector = new InjectedValue<>();
    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> transactionManagerInjector = new InjectedValue<>();
    private final InjectedValue<XAResourceRecoveryRegistry> xaResourceRecoveryRegistryInjector = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> serverEnvironmentInjector = new InjectedValue<>();

    public LocalTransactionContextService() {
    }

    public void start(final StartContext context) throws StartException {
        JBossLocalTransactionProvider.Builder builder = JBossLocalTransactionProvider.builder();
        builder.setExtendedJBossXATerminator(extendedJBossXATerminatorInjector.getValue());
        builder.setTransactionManager(transactionManagerInjector.getValue().getTransactionManager());
        builder.setTransactionSynchronizationRegistry(transactionManagerInjector.getValue().getTransactionSynchronizationRegistry());
        builder.setXATerminator(transactionManagerInjector.getValue().getJbossXATerminator());
        builder.setXAResourceRecoveryRegistry(xaResourceRecoveryRegistryInjector.getValue());
        builder.setXARecoveryLogDirRelativeToPath(serverEnvironmentInjector.getValue().getServerDataDir().toPath());
        final LocalTransactionContext transactionContext = this.context = new LocalTransactionContext(builder.build());
        // TODO: replace this with per-CL settings for embedded use and to support remote UserTransaction
        doPrivileged((PrivilegedAction<Void>) () -> {
            LocalTransactionContext.getContextManager().setGlobalDefault(transactionContext);
            return null;
        });
    }

    public void stop(final StopContext context) {
        this.context = null;
        // TODO: replace this with per-CL settings for embedded use and to support remote UserTransaction
        doPrivileged((PrivilegedAction<Void>) () -> {
            LocalTransactionContext.getContextManager().setGlobalDefault(null);
            return null;
        });
    }

    public InjectedValue<ExtendedJBossXATerminator> getExtendedJBossXATerminatorInjector() {
        return extendedJBossXATerminatorInjector;
    }

    public InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTransactionManagerInjector() {
        return transactionManagerInjector;
    }

    public InjectedValue<XAResourceRecoveryRegistry> getXAResourceRecoveryRegistryInjector() {
        return xaResourceRecoveryRegistryInjector;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentInjector() {
        return serverEnvironmentInjector;
    }

    public LocalTransactionContext getValue() throws IllegalStateException, IllegalArgumentException {
        return context;
    }
}
