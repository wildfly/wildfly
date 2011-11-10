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

package org.jboss.as.ejb3.cache;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.jca.core.spi.transaction.TxUtils;

/**
 * {@link StatefulObjectFactory} decorator that destroys its object on transaction completion, if necessary
 * @author Paul Ferraro
 */
public class TransactionAwareObjectFactory<T> implements StatefulObjectFactory<T> {
    private final StatefulObjectFactory<T> factory;
    private final TransactionManager tm;

    public TransactionAwareObjectFactory(StatefulObjectFactory<T> factory, TransactionManager tm) {
        this.factory = factory;
        this.tm = tm;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.StatefulObjectFactory#createInstance()
     */
    @Override
    public T createInstance() {
        return this.factory.createInstance();
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.StatefulObjectFactory#destroyInstance(java.lang.Object)
     */
    @Override
    public void destroyInstance(T instance) {
        final Transaction tx = this.getCurrentTransaction();
        if ((tx != null) && TxUtils.isActive(tx)) {
            try {
                // A transaction is in progress, so register a Synchronization so that the session can be destroyed on tx completion.
                tx.registerSynchronization(new DestroySynchronization<T>(this.factory, instance));
            } catch (RollbackException e) {
                throw new RuntimeException(e);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.factory.destroyInstance(instance);
        }
    }

    private Transaction getCurrentTransaction() {
        try {
            return this.tm.getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DestroySynchronization<T> implements Synchronization {
        private final StatefulObjectFactory<T> factory;
        private final T instance;

        DestroySynchronization(StatefulObjectFactory<T> factory, T instance) {
            this.factory = factory;
            this.instance = instance;
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int arg0) {
            this.factory.destroyInstance(this.instance);
        }
    }
}
