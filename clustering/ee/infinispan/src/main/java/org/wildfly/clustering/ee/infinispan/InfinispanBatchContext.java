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
package org.wildfly.clustering.ee.infinispan;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.commons.CacheException;
import org.wildfly.clustering.ee.BatchContext;

/**
 * A {@link BatchContext} that performs transaction context switching.
 * @author Paul Ferraro
 */
public class InfinispanBatchContext implements BatchContext {
    private final TransactionManager tm;
    private final Transaction existingTx;
    private final Transaction tx;

    InfinispanBatchContext(TransactionManager tm, Transaction tx) throws SystemException, InvalidTransactionException {
        this.tm = tm;
        this.tx = tx;
        // Switch transaction context
        this.existingTx = this.tm.suspend();
        this.tm.resume(this.tx);
    }

    @Override
    public void close() {
        // Restore previous transaction context, if necessary
        if ((this.existingTx != null) && !this.existingTx.equals(this.tx)) {
            try {
                this.tm.resume(this.existingTx);
            } catch (InvalidTransactionException | SystemException e) {
                throw new CacheException(e);
            }
        }
    }
}