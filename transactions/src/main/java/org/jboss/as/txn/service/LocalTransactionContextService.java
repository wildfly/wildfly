/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.LOCAL_PROVIDER_CAPABILITY;

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
    private final int staleTransactionTime;
    private JBossLocalTransactionProvider provider;

    public LocalTransactionContextService(final int staleTransactionTime) {
        this.staleTransactionTime = staleTransactionTime;
    }

    public void start(final StartContext context) throws StartException {
        JBossLocalTransactionProvider.Builder builder = JBossLocalTransactionProvider.builder();
        builder.setExtendedJBossXATerminator(extendedJBossXATerminatorInjector.getValue());
        builder.setTransactionManager(transactionManagerInjector.getValue().getTransactionManager());
        builder.setXAResourceRecoveryRegistry(xaResourceRecoveryRegistryInjector.getValue());
        builder.setXARecoveryLogDirRelativeToPath(serverEnvironmentInjector.getValue().getServerDataDir().toPath());
        builder.setStaleTransactionTime(staleTransactionTime);
        this.provider = builder.build();
        final LocalTransactionContext transactionContext = this.context = new LocalTransactionContext(this.provider);
        // TODO: replace this with per-CL settings for embedded use and to support remote UserTransaction
        doPrivileged((PrivilegedAction<Void>) () -> {
            LocalTransactionContext.getContextManager().setGlobalDefault(transactionContext);
            return null;
        });

        // Install the void service required by capability org.wildfly.transactions.global-default-local-provider
        // so other capabilities that require it can start their services after this capability
        // has completed its work.
        context.getChildTarget().addService(LOCAL_PROVIDER_CAPABILITY.getCapabilityServiceName())
                .setInstance(Service.NULL)
                .install();
    }

    public void stop(final StopContext context) {
        this.provider.removeXAResourceRecovery(xaResourceRecoveryRegistryInjector.getValue());
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
