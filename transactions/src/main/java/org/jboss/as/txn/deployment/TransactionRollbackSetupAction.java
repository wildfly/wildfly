/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

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
