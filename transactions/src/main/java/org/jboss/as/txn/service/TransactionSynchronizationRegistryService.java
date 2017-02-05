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

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;

/**
 * Service that exposes the TransactionSynchronizationRegistry
 *
 * @author Stuart Douglas
 */
public class TransactionSynchronizationRegistryService extends AbstractService<TransactionSynchronizationRegistry> {
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY;

    private static final TransactionSynchronizationRegistryService INSTANCE = new TransactionSynchronizationRegistryService();

    private TransactionSynchronizationRegistryService() {
    }

    public static ServiceController<TransactionSynchronizationRegistry> addService(final ServiceTarget target) {
        ServiceBuilder<TransactionSynchronizationRegistry> serviceBuilder = target.addService(SERVICE_NAME, INSTANCE);
        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        return serviceBuilder.install();
    }

    @Override
    public TransactionSynchronizationRegistry getValue() throws IllegalStateException {
        return ContextTransactionSynchronizationRegistry.getInstance();
    }
}
