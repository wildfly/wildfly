/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.chunk;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

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
