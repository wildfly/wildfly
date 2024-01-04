/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.chunk;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.partition.PartitionAnalyzer;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public final class ChunkPartitionAnalyzer implements PartitionAnalyzer {
    @Inject
    private StepContext stepContext;

    @Inject @BatchProperty(name = "thread.count")
    private int threadCount;

    @Inject @BatchProperty(name = "skip.thread.check")
    private boolean skipThreadCheck;

    private final Set<Long> childThreadIds = new HashSet<Long>();
    private int numOfCompletedPartitions;

    @Override
    public void analyzeCollectorData(final Serializable data) throws Exception {
        childThreadIds.add((Long) data);
    }

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        //the check for number of threads used is not very accurate.  The underlying thread pool
        //may choose a cold thread even when a warm thread has already been returned to pool and available.
        //especially when thread.count is 1, there may be 2 or more threads being used, but at one point,
        //there should be only 1 active thread running partition.
        numOfCompletedPartitions++;
        if(numOfCompletedPartitions == 3  && !skipThreadCheck) { //partitions in job xml
            if (childThreadIds.size() <= threadCount) {  //threads in job xml
                stepContext.setExitStatus(String.format("PASS: Max allowable thread count %s, actual threads %s",
                        threadCount, childThreadIds.size()));
            } else {
                stepContext.setExitStatus(String.format("FAIL: Expecting max thread count %s, but got %s",
                        threadCount, childThreadIds.size()));
            }
        }
    }
}
