/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.transaction;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

@Named
public class TransactedReader extends AbstractItemReader {
    private static final Logger logger = Logger.getLogger(TransactedReader.class);

    @Inject
    @BatchProperty(name = "job.timeout")
    private int timeout;

    @Inject
    private TransactedService transactedService;

    @Override
    public Object readItem() throws Exception {
        // one can check the log to verify which batch thread is been used to
        // run this step or partition
        logger.info("About to read item, job.timeout: " + timeout);
        transactedService.query(timeout);
        return null;
    }

}
