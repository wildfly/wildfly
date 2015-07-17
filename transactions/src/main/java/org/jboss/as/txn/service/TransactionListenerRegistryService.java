/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.*;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.listener.TransactionListenerRegistry;

import javax.transaction.UserTransaction;

/**
 * Service responsible for exposing a {@link org.jboss.tm.listener.TransactionListenerRegistry} instance.
 */
public class TransactionListenerRegistryService extends AbstractService<TransactionListenerRegistry> {
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_TRANSACTION_LISTENER_REGISTRY;

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> injectedArjunaTM = new InjectedValue<>();

    public static ServiceController<TransactionListenerRegistry> addService(final ServiceTarget target) {
        TransactionListenerRegistryService service = new TransactionListenerRegistryService();
        ServiceBuilder<TransactionListenerRegistry> serviceBuilder = target.addService(SERVICE_NAME, service);
        serviceBuilder.addDependency(ArjunaTransactionManagerService.SERVICE_NAME, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, service.injectedArjunaTM);
        return serviceBuilder.install();
    }

    @Override
    public TransactionListenerRegistry getValue() throws IllegalStateException {
        return (TransactionListenerRegistry) injectedArjunaTM.getValue().getTransactionManager();
    }
}
