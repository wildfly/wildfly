/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import jakarta.batch.api.partition.PartitionAnalyzer;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
public class TestPartitionAnalyzer implements PartitionAnalyzer {
    @Override
    public void analyzeCollectorData(Serializable serializable) throws Exception {

    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String s) throws Exception {

    }
}
