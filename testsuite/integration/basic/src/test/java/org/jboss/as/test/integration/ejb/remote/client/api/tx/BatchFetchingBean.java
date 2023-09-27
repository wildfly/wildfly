/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import org.jboss.logging.Logger;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * User: jpai
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@Remote (BatchRetriever.class)
public class BatchFetchingBean implements BatchRetriever {

    private static final Logger logger = Logger.getLogger(BatchFetchingBean.class);

    @PersistenceContext(unitName = "ejb-client-tx-pu")
    private EntityManager entityManager;

    public Batch fetchBatch(final String batchName) {
        logger.trace("Fetching batch " + batchName);
        return this.entityManager.find(Batch.class, batchName);
    }
}
