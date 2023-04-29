/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.suspendejb;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Dependent
public class LongRunningBatchletWithEjb implements Batchlet {
    @Inject
    private SuspendBatchLocal suspendBean;

    private volatile boolean stopRequested;

    @Inject
    @BatchProperty(name = "max.seconds")
    private Integer maxSeconds;

    @Override
    public String process() throws Exception {
        final BatchStatus status = suspendBean.getStatus();
        if (status == null || status == BatchStatus.STOPPING || status == BatchStatus.STOPPED) {
            // this batchlet is being restarted, so just complete it
            suspendBean.setStatus(BatchStatus.COMPLETED);
            SuspendBatchRemote.logger.infof("Directly complete job execution with status: %s", BatchStatus.COMPLETED);
            return BatchStatus.COMPLETED.name();
        }

        long endAt = maxSeconds * 1000 + System.currentTimeMillis();
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (stopRequested) {
                suspendBean.setStatus(BatchStatus.STOPPED);
                SuspendBatchRemote.logger.info("About to bail out after receiving stop request");
                return BatchStatus.STOPPED.name();
            }
            SuspendBatchRemote.logger.infof("Waiting for stop request, or till max.seconds: %s", maxSeconds);
        } while (System.currentTimeMillis() <= endAt);

        suspendBean.setStatus(BatchStatus.FAILED);
        return BatchStatus.FAILED.name();
    }

    @Override
    public void stop() {
        stopRequested = true;
        suspendBean.setStatus(BatchStatus.STOPPING);
    }
}
