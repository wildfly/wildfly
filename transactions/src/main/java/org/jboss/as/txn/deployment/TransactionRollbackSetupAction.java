/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Setup action that makes sure that no transactions leak from EE requests
 *
 * @author Stuart Douglas
 */
public class TransactionRollbackSetupAction implements SetupAction, Service<TransactionRollbackSetupAction> {

    private static final ThreadLocal<Holder> depth = new ThreadLocal<Holder>();

    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();

    private final ServiceName serviceName;

    public TransactionRollbackSetupAction(final ServiceName serviceName) {
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
        final TransactionManager tm = transactionManager.getOptionalValue();
        if (tm != null) {
            try {
                tm.setTransactionTimeout(0);
            } catch (Exception ignore) {
            }
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

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public TransactionRollbackSetupAction getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<TransactionManager> getTransactionManager() {
        return transactionManager;
    }

    private boolean changeDepth(int increment) {
        Holder holder = depth.get();
        if (holder == null) {
            //if there is a transaction active initially we just track the depth
            //and don't actually close it, because we don't 'own' the transaction
            //this can happen when running async listeners outside the context of a request
            holder = new Holder();
            try {
                final TransactionManager tm = transactionManager.getOptionalValue();
                if (tm != null) {
                    holder.actuallyCleanUp = !isTransactionActive(tm, tm.getStatus());
                }
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
            final TransactionManager tm = transactionManager.getOptionalValue();
            if (tm == null) {
                return;
            }
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
