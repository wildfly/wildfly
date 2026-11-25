/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.LOCAL_PROVIDER_CAPABILITY;

import java.security.PrivilegedAction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.ExtendedJBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.jboss.JBossLocalTransactionProvider;

/**
 * The service which provides the {@link LocalTransactionContext} for the server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class LocalTransactionContextService implements Service<LocalTransactionContext> {
    private final Consumer<LocalTransactionContext> contextConsumer;
    private final Supplier<ExtendedJBossXATerminator> extendedJBossXATerminatorSupplier;
    private final Supplier<com.arjuna.ats.jbossatx.jta.TransactionManagerService> transactionManagerSupplier;
    private final Supplier<XAResourceRecoveryRegistry> xaResourceRecoveryRegistrySupplier;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
    private final int staleTransactionTime;
    private volatile LocalTransactionContext context;
    private JBossLocalTransactionProvider provider;

    public LocalTransactionContextService(final Consumer<LocalTransactionContext> contextConsumer,
                                          final Supplier<ExtendedJBossXATerminator> extendedJBossXATerminatorSupplier,
                                          final Supplier<com.arjuna.ats.jbossatx.jta.TransactionManagerService> transactionManagerSupplier,
                                          final Supplier<XAResourceRecoveryRegistry> xaResourceRecoveryRegistrySupplier,
                                          final Supplier<ServerEnvironment> serverEnvironmentSupplier,
                                          final int staleTransactionTime) {
        this.contextConsumer = contextConsumer;
        this.extendedJBossXATerminatorSupplier = extendedJBossXATerminatorSupplier;
        this.transactionManagerSupplier = transactionManagerSupplier;
        this.xaResourceRecoveryRegistrySupplier = xaResourceRecoveryRegistrySupplier;
        this.serverEnvironmentSupplier = serverEnvironmentSupplier;
        this.staleTransactionTime = staleTransactionTime;
    }

    public void start(final StartContext context) throws StartException {
        JBossLocalTransactionProvider.Builder builder = JBossLocalTransactionProvider.builder();
        builder.setExtendedJBossXATerminator(extendedJBossXATerminatorSupplier.get());
        builder.setTransactionManager(transactionManagerSupplier.get().getTransactionManager());
        builder.setXAResourceRecoveryRegistry(xaResourceRecoveryRegistrySupplier.get());
        builder.setXARecoveryLogDirRelativeToPath(serverEnvironmentSupplier.get().getServerDataDir().toPath());
        builder.setStaleTransactionTime(staleTransactionTime);
        this.provider = builder.build();
        final LocalTransactionContext transactionContext = this.context = new LocalTransactionContext(this.provider);
        contextConsumer.accept(transactionContext);
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
        contextConsumer.accept(null);
        this.provider.removeXAResourceRecovery(xaResourceRecoveryRegistrySupplier.get());
        // TODO: replace this with per-CL settings for embedded use and to support remote UserTransaction
        doPrivileged((PrivilegedAction<Void>) () -> {
            LocalTransactionContext.getContextManager().setGlobalDefault(null);
            return null;
        });
    }

    @Override
    public LocalTransactionContext getValue() {
        return context;
    }
}
