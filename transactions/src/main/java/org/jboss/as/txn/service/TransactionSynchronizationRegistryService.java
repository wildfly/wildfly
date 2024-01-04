/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.service;

import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

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

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> injectedArjunaTM = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();


    private TransactionSynchronizationRegistryService() {
    }

    public static void addService(final CapabilityServiceTarget target) {
        TransactionSynchronizationRegistryService service = new TransactionSynchronizationRegistryService();
        ServiceBuilder<?> serviceBuilder = target.addCapability(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY);
        serviceBuilder.setInstance(service);
        serviceBuilder.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        serviceBuilder.addDependency(ArjunaTransactionManagerService.SERVICE_NAME, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, service.injectedArjunaTM);
        serviceBuilder.addAliases(INTERNAL_SERVICE_NAME);
        serviceBuilder.install();
    }

    @Override
    public void start(final StartContext startContext) {
        // noop
    }

    @Override
    public void stop(final StopContext stopContext) {
        // noop
    }

    @Override
    public TransactionSynchronizationRegistry getValue() throws IllegalStateException {
        return injectedArjunaTM.getValue().getTransactionSynchronizationRegistry();
    }
}
