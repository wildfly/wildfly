/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.io.IOException;
import java.security.AccessController;
import java.util.function.Function;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.transaction.TransactionUtil;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jipijapa.plugin.spi.ScopedStatelessSessionSupplier;
import org.wildfly.transaction.client.ContextTransactionManager;

public final class StatelessSessionSupplierImpl implements ScopedStatelessSessionSupplier {

    private static final long serialVersionUID = 455498112L;

    private final String puScopedName;          // Scoped name of the persistent unit

    private final Function<EntityManagerFactory, AutoCloseable> sessionFactory;
    private transient EntityManagerFactory emf;
    private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private transient TransactionManager transactionManager;

    public StatelessSessionSupplierImpl(EntityManagerFactory emf, Function<EntityManagerFactory, AutoCloseable> sessionFactory,
                                    String puScopedName) {
        this.emf = emf;
        this.sessionFactory = sessionFactory;
        this.puScopedName = puScopedName;
        initTransactionFields();
    }

    private void initTransactionFields() {
        transactionManager = ContextTransactionManager.getInstance();
        transactionSynchronizationRegistry = (TransactionSynchronizationRegistry) currentServiceContainer().getService(JPAServiceNames.TRANSACTION_SYNCHRONIZATION_REGISTRY_SERVICE).getValue();
    }

    @Override
    public AutoCloseable get() {
        AutoCloseable statelessSession;
        boolean isInTx;

        isInTx = TransactionUtil.isInTx(transactionManager);

        if (isInTx) {
            statelessSession = getOrCreateTransactionScopedStatelessSession();
        } else {
            statelessSession = NonTxEmCloser.get(AutoCloseable.class, puScopedName);
            if (statelessSession == null) {
                statelessSession = sessionFactory.apply(emf);
                NonTxEmCloser.add(puScopedName, statelessSession);
            }
        }
        return statelessSession;
    }

    /**
     * get or create a Transactional stateless session.
     * Only call while a transaction is active in the current thread.
     *
     * @return the StatelessSession
     */
    private AutoCloseable getOrCreateTransactionScopedStatelessSession() {
        AutoCloseable statelessSession = TransactionUtil.getScopedObjectInTransactionRegistry(AutoCloseable.class, puScopedName, transactionSynchronizationRegistry);
        if (statelessSession == null) {
            statelessSession = sessionFactory.apply(emf);
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debugf("%s: created entity manager session %s", TransactionUtil.getTransactionScopedObjectDetails(statelessSession, puScopedName),
                        TransactionUtil.getTransaction(transactionManager).toString());
            }
            TransactionUtil.registerSynchronization(statelessSession, puScopedName, transactionSynchronizationRegistry, transactionManager);
            TransactionUtil.putScopedObjectInTransactionRegistry(puScopedName, statelessSession, transactionSynchronizationRegistry);
        }
        return statelessSession;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // read all non-transient fields
        in.defaultReadObject();
        final ServiceController<?> controller = currentServiceContainer().getService(JPAServiceNames.getPUServiceName(puScopedName));
        final PersistenceUnitServiceImpl persistenceUnitService = (PersistenceUnitServiceImpl) controller.getService();
        initTransactionFields();

        emf = persistenceUnitService.getEntityManagerFactory();
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
