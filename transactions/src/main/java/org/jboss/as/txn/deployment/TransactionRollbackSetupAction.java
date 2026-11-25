/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Setup action that makes sure that no transactions leak from EE requests
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TransactionRollbackSetupAction implements SetupAction, Service {

    private static final ThreadLocal<Holder> depth = new ThreadLocal<Holder>();
    private final Consumer<TransactionRollbackSetupAction> txnRollbackSetupActionConsumer;
    private final Supplier<TransactionManager> transactionManagerSupplier;
    private final ServiceName serviceName;

    public TransactionRollbackSetupAction(final Consumer<TransactionRollbackSetupAction> txnRollbackSetupActionConsumer, final Supplier<TransactionManager> transactionManagerSupplier, final ServiceName serviceName) {
        this.txnRollbackSetupActionConsumer = txnRollbackSetupActionConsumer;
        this.transactionManagerSupplier = transactionManagerSupplier;
        this.serviceName = serviceName;
    }

    @Override
    public void setup(final Map<String, Object> properties) {
        changeDepth(1);
    }

    @Override
    public void teardown(final Map<String, Object> properties) {
        if (changeDepth(-1)) {
            checkTransactionStatus();
        }

        // reset transaction timeout to the default value
        final TransactionManager tm = transactionManagerSupplier.get();
        try {
            tm.setTransactionTimeout(0);
        } catch (Exception ignore) {
        }
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return Collections.singleton(serviceName);
    }

    @Override
    public void start(final StartContext context) throws StartException {
        txnRollbackSetupActionConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        txnRollbackSetupActionConsumer.accept(null);
    }

    private boolean changeDepth(int increment) {
        Holder holder = depth.get();
        if (holder == null) {
            //if there is a transaction active initially we just track the depth
            //and don't actually close it, because we don't 'own' the transaction
            //this can happen when running async listeners outside the context of a request
            holder = new Holder();
            try {
                final TransactionManager tm = transactionManagerSupplier.get();
                holder.actuallyCleanUp = !isTransactionActive(tm, tm.getStatus());
                depth.set(holder);
            } catch (Exception e) {
                TransactionLogger.ROOT_LOGGER.unableToGetTransactionStatus(e);
            }
        }

        holder.depth += increment;
        if (holder.depth == 0) {
            depth.set(null);
            return holder.actuallyCleanUp;
        }
        return false;
    }

    private void checkTransactionStatus() {
        try {
            final TransactionManager tm = transactionManagerSupplier.get();
            final int status = tm.getStatus();
            final boolean active = isTransactionActive(tm, status);
            if (active) {
                try {
                    TransactionLogger.ROOT_LOGGER.transactionStillOpen(status);
                    tm.rollback();
                } catch (Exception ex) {
                    TransactionLogger.ROOT_LOGGER.unableToRollBack(ex);
                }
            }
        } catch (Exception e) {
            TransactionLogger.ROOT_LOGGER.unableToGetTransactionStatus(e);
        }
    }

    private boolean isTransactionActive(TransactionManager tm, int status) throws SystemException {
        switch (status) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_PREPARING:
            case Status.STATUS_ROLLING_BACK:
            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_PREPARED:
                return true;
        }
        return false;
    }

    private static class Holder {
        int depth;
        boolean actuallyCleanUp = true;
    }
}
