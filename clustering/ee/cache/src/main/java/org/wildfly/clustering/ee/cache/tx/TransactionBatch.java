/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.tx;

import jakarta.transaction.Transaction;

import org.wildfly.clustering.ee.Batch;

/**
 * @author Paul Ferraro
 */
public interface TransactionBatch extends Batch {
    /**
     * Returns the transaction associated with this batch
     * @return a transaction
     */
    Transaction getTransaction();

    /**
     * Returns an interposed batch.
     */
    TransactionBatch interpose();
}
