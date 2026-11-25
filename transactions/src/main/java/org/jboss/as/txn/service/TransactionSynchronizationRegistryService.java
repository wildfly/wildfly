/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.service;

import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that exposes the TransactionSynchronizationRegistry
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionSynchronizationRegistryService implements Service<TransactionSynchronizationRegistry> {
    /** @deprecated Use the "org.wildfly.transactions.transaction-synchronization-registry" capability  */
    @Deprecated
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY;
    /** Non-deprecated service name only for use within the subsystem */
    @SuppressWarnings("deprecation")
    public static final ServiceName INTERNAL_SERVICE_NAME = TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY;
    private final Consumer<TransactionSynchronizationRegistry> txnRegistryConsumer;
    private final Supplier<com.arjuna.ats.jbossatx.jta.TransactionManagerService> arjunaTMSupplier;
    private volatile TransactionSynchronizationRegistry registry;

    private TransactionSynchronizationRegistryService(final Consumer<TransactionSynchronizationRegistry> txnRegistryConsumer,
                                                      final Supplier<com.arjuna.ats.jbossatx.jta.TransactionManagerService> arjunaTMSupplier) {
        this.txnRegistryConsumer = txnRegistryConsumer;
        this.arjunaTMSupplier = arjunaTMSupplier;
    }

    @Override
    public void start(final StartContext startContext) {
        registry = arjunaTMSupplier.get().getTransactionSynchronizationRegistry();
        txnRegistryConsumer.accept(registry);
    }

    @Override
    public void stop(final StopContext stopContext) {
        txnRegistryConsumer.accept(null);
    }

    @Override
    public TransactionSynchronizationRegistry getValue() {
        return registry;
    }

    public static void addService(final CapabilityServiceTarget target) {
        final CapabilityServiceBuilder<?> sb = target.addService();
        final Consumer<TransactionSynchronizationRegistry> txnRegistryConsumer = sb.provides(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY, INTERNAL_SERVICE_NAME);
        final Supplier<com.arjuna.ats.jbossatx.jta.TransactionManagerService> arjunaTMSupplier = sb.requires(ArjunaTransactionManagerService.SERVICE_NAME);
        sb.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        sb.setInstance(new TransactionSynchronizationRegistryService(txnRegistryConsumer, arjunaTMSupplier));
        sb.install();
    }
}
