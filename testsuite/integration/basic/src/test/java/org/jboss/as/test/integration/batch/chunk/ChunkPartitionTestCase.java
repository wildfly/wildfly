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

import java.net.URL;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ChunkPartitionTestCase extends AbstractBatchTestCase {
    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("batch-chunk-partition.war", ChunkPartitionTestCase.class.getPackage(), "chunkPartition.xml")
                .addPackage(ChunkPartitionTestCase.class.getPackage());
    }

    @Test
    public void chunks() throws Exception {
        for (int i = 10; i >= 8; i--) {
            final UrlBuilder builder = UrlBuilder.of(url, "start");
            builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "chunkPartition");
            builder.addParameter("thread.count", i);
            builder.addParameter("writer.sleep.time", 100);
            final String result = performCall(builder.build());
            final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);


            Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
            // final String exitStatus = stepExecution0.getExitStatus();
            // System.out.printf("Step exit status: %s%n", exitStatus);
            // Assert.assertTrue(exitStatus.startsWith("PASS"));
        }

        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "chunkPartition");
        builder.addParameter("thread.count", 1);
        builder.addParameter("skip.thread.check", "true");
        builder.addParameter("writer.sleep.time", 0);
        final String result = performCall(builder.build());
        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

}
