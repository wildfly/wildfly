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
package org.jboss.as.ee.datasource;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Proxy handler that automatically enlists XA resources returned from a datasource.
 *
 * If getConnection() is called and the datasource is an XA datasource then getXAConnection() is called
 * instead and the resource is enlised in the transaction
 *
 * @author Stuart Douglas
 */
public class DataSourceTransactionProxyHandler implements InvocationHandler, Synchronization {

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry synchronizationRegistry;

    private final Object delegate;

    private final ThreadLocal<Boolean> synchronizationRegistered = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public DataSourceTransactionProxyHandler(final Object delegate, final TransactionManager transactionManager, final TransactionSynchronizationRegistry synchronizationRegistry) {
        this.transactionManager = transactionManager;
        this.delegate = delegate;
        this.synchronizationRegistry = synchronizationRegistry;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getName().equals("getConnection") && method.getParameterTypes().length == 0 && delegate instanceof XADataSource) {
            final XADataSource xa = (XADataSource) delegate;
            final XAConnection xacon = xa.getXAConnection();
            if (!synchronizationRegistered.get()) {
                if (transactionManager.getTransaction() != null && transactionActive(transactionManager.getTransaction().getStatus())) {
                    transactionManager.getTransaction().enlistResource(xacon.getXAResource());
                    synchronizationRegistry.registerInterposedSynchronization(this);
                    synchronizationRegistered.set(true);
                }
            }
            return xacon.getConnection();
        } else {
            final Object ret = method.invoke(delegate, args);
            return ret;
        }
    }

    private boolean transactionActive(final int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_COMMITTING:
                return true;
        }
        return false;
    }


    @Override
    public void beforeCompletion() {
    }

    @Override
    public void afterCompletion(final int status) {
        synchronizationRegistered.remove();
    }
}
