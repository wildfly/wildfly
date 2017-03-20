/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.batch.runtime.JobExecution;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * Test for JBERET-231/JBEAP-4811.
 * When a batch job fails due to transaction timeout, another job should be able to run on the same thread.
 */
@RunWith(Arquillian.class)
@ServerSetup(SingleThreadedBatchSetup.class)
public class BatchTransactionTimeoutTestCase extends AbstractBatchTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("batch-transaction-timeout.war", BatchTransactionTimeoutTestCase.class.getPackage(), "timeout-job.xml")
                .addPackage(BatchTransactionTimeoutTestCase.class.getPackage())
                .addAsResource("org/jboss/as/test/integration/batch/transaction/persistence.xml", "META-INF/persistence.xml");
    }

    @RunAsClient
    @Test
    public void testThreadIsAvailableForNextJob(@ArquillianResource final URL url) throws Exception {
        assertEquals("FAILED", executeJobWithTimeout(url));

        assertEquals("COMPLETED", executeJobWithoutTimout(url));
    }

    private String executeJobWithTimeout(@ArquillianResource URL url) throws ExecutionException, IOException, TimeoutException {
        return executeJob(url, 10000);
    }

    private String executeJobWithoutTimout(@ArquillianResource URL url) throws ExecutionException, IOException, TimeoutException {
        return executeJob(url, -1);
    }

    private String executeJob(@ArquillianResource URL url, int timeout) throws ExecutionException, IOException, TimeoutException {
        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "timeout-job");
        builder.addParameter("job.timeout", timeout);

        final String result = performCall(builder.build(), 20);
        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);

        return jobExecution.getExitStatus();
    }
}
