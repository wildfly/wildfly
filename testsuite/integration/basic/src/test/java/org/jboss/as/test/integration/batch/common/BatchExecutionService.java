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

package org.jboss.as.test.integration.batch.common;

import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.ejb.Singleton;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Singleton
public class BatchExecutionService {

    private final JobOperator jobOperator;

    public BatchExecutionService() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    public JobOperator getJobOperator() {
        return jobOperator;
    }

    public JobExecution start(final String jobXml, final Properties params) {
        final long id = jobOperator.start(jobXml, params);
        return jobOperator.getJobExecution(id);
    }
}