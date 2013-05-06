/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.context.Flag;

/**
 * Cache invoker that invokes an operation within the context of a new transaction.
 * @author Paul Ferraro
 */
public class TransactionCacheInvoker implements CacheInvoker {

    private final CacheInvoker invoker;

    public TransactionCacheInvoker(CacheInvoker invoker) {
        this.invoker = invoker;
    }

    public TransactionCacheInvoker() {
        this(new SimpleCacheInvoker());
    }

    @Override
    public <K, V, R> R invoke(Cache<K, V> cache, Operation<K, V, R> operation, Flag... flags) {
        TransactionManager tm = cache.getAdvancedCache().getTransactionManager();
        try {
            tm.begin();
            try {
                R result = this.invoker.invoke(cache, operation, flags);
                try {
                    tm.commit();
                } catch (RollbackException e) {
                    throw new CacheException(e);
                } catch (HeuristicMixedException e) {
                    throw new CacheException(e);
                } catch (HeuristicRollbackException e) {
                    throw new CacheException(e);
                }
                return result;
            } catch (RuntimeException e) {
                tm.rollback();
                throw e;
            }
        } catch (SystemException e) {
            throw new CacheException(e);
        } catch (NotSupportedException e) {
            throw new CacheException(e);
        }
    }
}
