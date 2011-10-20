/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.as.clustering.web.BatchingManager;

import static org.jboss.as.clustering.web.impl.ClusteringWebLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.web.impl.ClusteringWebMessages.MESSAGES;

/**
 * @author Paul Ferraro
 */
public class TransactionBatchingManager implements BatchingManager {

    private final TransactionManager tm;

    public TransactionBatchingManager(TransactionManager tm) {
        this.tm = tm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBatchInProgress() throws Exception {
        return this.tm.getTransaction() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startBatch() throws Exception {
        this.tm.begin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBatchRollbackOnly() throws Exception {
        this.tm.setRollbackOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endBatch() {
        try {
            if (this.tm.getTransaction().getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                this.tm.commit();
            } else {
                ROOT_LOGGER.debug("endBatch(): rolling back batch");

                this.tm.rollback();
            }
        } catch (RollbackException e) {
            // Do nothing here since cache may rollback automatically.
            ROOT_LOGGER.rollingBackTransaction(e, "endBatch()");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.caughtExceptionEndingBatch(e, "endTransaction()");
        }
    }
}
