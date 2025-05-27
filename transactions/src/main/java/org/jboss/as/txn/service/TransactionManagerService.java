/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.TransactionManager;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.AssociationListener;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.CreationListener;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Service responsible for getting the {@link TransactionManager}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionManagerService implements Service {

    /** @deprecated Use the "org.wildfly.transactions.global-default-local-provider" capability to confirm existence of a local provider
     *              and org.wildfly.transaction.client.ContextTransactionManager to obtain a TransactionManager reference. */
    @Deprecated
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_TRANSACTION_MANAGER;
    /** Non-deprecated service name only for use within the subsystem */
    @SuppressWarnings("deprecation")
    public static final ServiceName INTERNAL_SERVICE_NAME = TxnServices.JBOSS_TXN_TRANSACTION_MANAGER;
    private final Consumer<TransactionManager> txnManagerConsumer;
    private final Supplier<UserTransactionRegistry> registrySupplier;

    private TransactionManagerService(final Consumer<TransactionManager> txnManagerConsumer, final Supplier<UserTransactionRegistry> registrySupplier) {
        this.txnManagerConsumer = txnManagerConsumer;
        this.registrySupplier = registrySupplier;
    }

    public void start(final StartContext context) throws StartException {
        final UserTransactionRegistry registry = registrySupplier.get();

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
        txnManagerConsumer.accept(ContextTransactionManager.getInstance());
    }

    @Override
    public void stop(final StopContext stopContext) {
        txnManagerConsumer.accept(null);
    }

    public static void addService(final ServiceTarget target) {
        final ServiceBuilder<?> sb = target.addService();
        final Consumer<TransactionManager> txnManagerConsumer = sb.provides(INTERNAL_SERVICE_NAME);
        final Supplier<UserTransactionRegistry> registrySupplier = sb.requires(UserTransactionRegistryService.SERVICE_NAME);
        // This is really a dependency on the global context.  TODO: Break this later; no service is needed for TM really
        sb.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
        sb.setInstance(new TransactionManagerService(txnManagerConsumer, registrySupplier));
        sb.install();
    }
}
