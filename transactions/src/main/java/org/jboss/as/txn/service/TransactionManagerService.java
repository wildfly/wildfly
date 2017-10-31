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

package org.jboss.as.txn.service;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.AssociationListener;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.CreationListener;
import org.wildfly.transaction.client.LocalTransactionContext;

import javax.transaction.TransactionManager;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for getting the {@link TransactionManager}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class TransactionManagerService extends AbstractService<TransactionManager> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_TRANSACTION_MANAGER;

    private InjectedValue<UserTransactionRegistry> registryInjector = new InjectedValue<>();

    private TransactionManagerService() {
    }

    public static ServiceController<TransactionManager> addService(final ServiceTarget target) {
        final TransactionManagerService service = new TransactionManagerService();
        ServiceBuilder<TransactionManager> serviceBuilder = target.addService(SERVICE_NAME, service);
        // This is really a dependency on the global context.  TODO: Break this later; no service is needed for TM really
        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        serviceBuilder.addDependency(UserTransactionRegistryService.SERVICE_NAME, UserTransactionRegistry.class, service.registryInjector);
        return serviceBuilder.install();
    }

    public void start(final StartContext context) throws StartException {
        final UserTransactionRegistry registry = registryInjector.getValue();

        LocalTransactionContext.getCurrent().registerCreationListener((txn, createdBy) -> {
            if (createdBy == CreationListener.CreatedBy.USER_TRANSACTION) {
                if (WildFlySecurityManager.isChecking()) {
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        txn.registerAssociationListener(new AssociationListener() {
                            private final AtomicBoolean first = new AtomicBoolean();

                            public void associationChanged(final AbstractTransaction t, final boolean a) {
                                if (a && first.compareAndSet(false, true)) registry.userTransactionStarted();
                            }
                        });
                        return null;
                    });
                } else {
                    txn.registerAssociationListener(new AssociationListener() {
                        private final AtomicBoolean first = new AtomicBoolean();

                        public void associationChanged(final AbstractTransaction t, final boolean a) {
                            if (a && first.compareAndSet(false, true)) registry.userTransactionStarted();
                        }
                    });
                }
            }
        });
    }

    @Override
    public TransactionManager getValue() throws IllegalStateException {
        return ContextTransactionManager.getInstance();
    }
}
