/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that exposes the TransactionSynchronizationRegistry
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionSynchronizationRegistryService extends AbstractService<TransactionSynchronizationRegistry> {
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
    public TransactionSynchronizationRegistry getValue() throws IllegalStateException {
        return injectedArjunaTM.getValue().getTransactionSynchronizationRegistry();
    }
}
