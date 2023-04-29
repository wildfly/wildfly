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

import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.inject.Inject;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class SuspendBatchSingleton implements SuspendBatchRemote, SuspendBatchLocal {

    @Inject
    private JobOperator jobOperator;

    private BatchStatus status;
    @Override
    public synchronized void setStatus(final BatchStatus status) {
        this.status = status;
    }

    @Override
    public synchronized BatchStatus getStatus() {
        return status;
    }

    @Override
    public void startJob(final String jobXmlName, final int maxSeconds) {
        setStatus(BatchStatus.STARTING);
        final Properties properties = new Properties();
        properties.setProperty("max.seconds", String.valueOf(maxSeconds));
        jobOperator.start(jobXmlName, properties);
        SuspendBatchRemote.logger.infof("Starting job %s with maxSeconds %s", jobXmlName, maxSeconds);
    }
}
