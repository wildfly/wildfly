/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.transactions;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.as.osgi.AbstractSubsystemExtension;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.IntegrationService;
import org.osgi.framework.BundleContext;

/**
 * A Transactions extension to the OSGi subsystem
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Jul-2012
 */
public class TransactionsExtension extends AbstractSubsystemExtension {

    private final InjectedValue<TransactionManager> injectedTransactionManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<UserTransaction> injectedUserTransaction = new InjectedValue<UserTransaction>();

    @Override
    public void configureServiceDependencies(ServiceName serviceName, ServiceBuilder<?> builder) {
        if (serviceName.equals(IntegrationService.SYSTEM_SERVICES_PLUGIN)) {
            builder.addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, injectedTransactionManager);
            builder.addDependency(UserTransactionService.SERVICE_NAME, UserTransaction.class, injectedUserTransaction);
        }
    }

    @Override
    public void startSystemServices(StartContext startContext, BundleContext systemContext) {
        // Register the {@link TransactionManager} and {@link UserTransaction} services
        systemContext.registerService(TransactionManager.class.getName(), injectedTransactionManager.getValue(), null);
        systemContext.registerService(UserTransaction.class.getName(), injectedUserTransaction.getValue(), null);
    }
}
