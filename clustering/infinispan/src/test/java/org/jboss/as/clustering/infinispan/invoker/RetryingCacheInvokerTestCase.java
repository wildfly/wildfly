/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.invoker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.remoting.transport.jgroups.SuspectException;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.TimeoutException;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class RetryingCacheInvokerTestCase {
    private final TransactionManager tm = mock(TransactionManager.class);
    private final CacheInvoker invoker = mock(CacheInvoker.class);
    private final AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
    private final CacheInvoker.Operation<Object, Object, Object> operation = mock(CacheInvoker.Operation.class);
    private final CacheInvoker subject = new RetryingCacheInvoker(this.invoker, 0);

    @Test
    public void nonTransactionalRetry() {
        this.nonTransactionalRetry(new TimeoutException(), true);
        this.nonTransactionalRetry(new SuspectException(), true);
        this.nonTransactionalRetry(new CacheException(), false);
    }

    private void nonTransactionalRetry(Throwable exception, boolean allowsRetry) {
        Flag flag = Flag.FORCE_SYNCHRONOUS;
        Object expected = new Object();
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();

        when(this.cache.getCacheConfiguration()).thenReturn(config);
        when(this.invoker.invoke(this.cache, this.operation, flag)).thenThrow(exception).thenReturn(expected);

        try {
            Object result = this.subject.invoke(this.cache, this.operation, flag);
            if (allowsRetry) {
                assertSame(expected, result);
            } else {
                fail("Retry should not have been allowed for exception: " + exception.getClass().getName());
            }
        } catch (RuntimeException e) {
            if (!allowsRetry) {
                assertSame(exception, e);
            } else {
                fail("Retry should have been allowed for exception: " + exception.getClass().getName());
            }
        }
    }

    @Test
    public void transactionalRetry() throws SystemException {
        this.transactionalRetry(Status.STATUS_ACTIVE, true);
        this.transactionalRetry(Status.STATUS_COMMITTED, false);
        this.transactionalRetry(Status.STATUS_COMMITTING, true);
        this.transactionalRetry(Status.STATUS_MARKED_ROLLBACK, false);
        this.transactionalRetry(Status.STATUS_NO_TRANSACTION, true);
        this.transactionalRetry(Status.STATUS_PREPARED, false);
        this.transactionalRetry(Status.STATUS_PREPARING, true);
        this.transactionalRetry(Status.STATUS_ROLLEDBACK, false);
        this.transactionalRetry(Status.STATUS_ROLLING_BACK, false);
        this.transactionalRetry(Status.STATUS_UNKNOWN, false);
    }

    private void transactionalRetry(int status, boolean allowsRetry) throws SystemException {
        Flag flag = Flag.FORCE_SYNCHRONOUS;
        Throwable expectedException = new TimeoutException();
        Object expected = new Object();
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getCacheConfiguration()).thenReturn(config);
        when(this.cache.getTransactionManager()).thenReturn(this.tm);
        when(this.tm.getStatus()).thenReturn(status);
        when(this.invoker.invoke(this.cache, this.operation, flag)).thenThrow(expectedException).thenReturn(expected);

        try {
            Object result = this.subject.invoke(this.cache, this.operation, flag);
            if (allowsRetry) {
                assertSame(expected, result);
            } else {
                fail("Retry should not have been allowed for tx status: " + status);
            }
        } catch (RuntimeException e) {
            if (!allowsRetry) {
                assertSame(expectedException, e);
            } else {
                fail("Retry should have been allowed for tx status: " + status);
            }
        }
    }
}
