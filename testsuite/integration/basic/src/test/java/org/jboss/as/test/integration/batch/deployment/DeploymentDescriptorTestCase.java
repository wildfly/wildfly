/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.deployment;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Runs tests with a {@code jboss-all.xml} deployment descriptor.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(DeploymentDescriptorTestCase.JdbcJobRepositorySetUp.class)
public class DeploymentDescriptorTestCase extends AbstractBatchTestCase {

    // in-memory deployment names
    private static final String NAMED_IN_MEMORY_DEPLOYMENT = "batch-named-in-memory.war";
    private static final String DEFINED_IN_MEMORY_DEPLOYMENT = "batch-defined-in-memory.war";

    // JDBC deployment names
    private static final String NAMED_JDBC_DEPLOYMENT = "batch-named-jdbc.war";

    @Deployment(name = NAMED_IN_MEMORY_DEPLOYMENT)
    public static WebArchive createNamedInMemoryDeployment() {
        return createDefaultWar(NAMED_IN_MEMORY_DEPLOYMENT, DeploymentDescriptorTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class)
                .addAsManifestResource(DeploymentDescriptorTestCase.class.getPackage(), "named-in-memory-jboss-all.xml", "jboss-all.xml");
    }

    @Deployment(name = DEFINED_IN_MEMORY_DEPLOYMENT)
    public static WebArchive createDefinedInMemoryDeployment() {
        return createDefaultWar(DEFINED_IN_MEMORY_DEPLOYMENT, DeploymentDescriptorTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class)
                .addAsManifestResource(DeploymentDescriptorTestCase.class.getPackage(), "defined-in-memory-jboss-all.xml", "jboss-all.xml");
    }

    @Deployment(name = NAMED_JDBC_DEPLOYMENT)
    public static WebArchive createNamedJdbcDeployment() {
        return createDefaultWar(NAMED_JDBC_DEPLOYMENT, DeploymentDescriptorTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class)
                .addAsManifestResource(DeploymentDescriptorTestCase.class.getPackage(), "named-jdbc-jboss-all.xml", "jboss-all.xml");
    }

    @OperateOnDeployment(NAMED_IN_MEMORY_DEPLOYMENT)
    @Test
    public void namedInMemoryTest(@ArquillianResource final URL url) throws Exception {
        // Test the default batch defined, ExampleDS, repository is isolated
        testCompletion(1L, url);
        testCompletion(2L, url);
    }

    @OperateOnDeployment(DEFINED_IN_MEMORY_DEPLOYMENT)
    @Test
    public void definedInMemoryTest(@ArquillianResource final URL url) throws Exception {
        // Test that a newly named
        testCompletion(1L, url);
        testCompletion(2L, url);
    }

    @OperateOnDeployment(NAMED_JDBC_DEPLOYMENT)
    @Test
    public void namedJdbcTest(@ArquillianResource final URL url) throws Exception {
        // Test the default batch defined, ExampleDS, repository is isolated
        testCompletion(1L, url);
        testCompletion(2L, url);
    }

    private void testCompletion(final long expectedExecutionId, final URL url) throws IOException, ExecutionException, TimeoutException {
        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "test-chunk");
        builder.addParameter("reader.end", 10);
        final String result = performCall(builder.build());
        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertEquals(expectedExecutionId, jobExecution.getExecutionId());
    }


    static class JdbcJobRepositorySetUp extends SnapshotRestoreSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
            final CompositeOperationBuilder operationBuilder = CompositeOperationBuilder.create();

            // Add a new data-source
            ModelNode address = Operations.createAddress("subsystem", "datasources", "data-source", "batch-ds");
            ModelNode op = Operations.createAddOperation(address);
            op.get("driver-name").set("h2");
            op.get("jndi-name").set("java:jboss/datasources/batch");
            op.get("connection-url").set("jdbc:h2:mem:batch-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            operationBuilder.addStep(op);

            // Add a new JDBC job repository with the new data-source
            address = Operations.createAddress("subsystem", "batch-jberet", "jdbc-job-repository", "batch-ds");
            op = Operations.createAddOperation(address);
            op.get("data-source").set("batch-ds");
            operationBuilder.addStep(op);

            // Add a new in-memory repository
            address = Operations.createAddress("subsystem", "batch-jberet", "in-memory-job-repository", "batch-in-mem");
            operationBuilder.addStep(Operations.createAddOperation(address));

            // Add a new thread-pool
            address = Operations.createAddress("subsystem", "batch-jberet", "thread-pool", "deployment-thread-pool");
            op = Operations.createAddOperation(address);
            op.get("max-threads").set(5L);
            final ModelNode keepAlive = op.get("keepalive-time");
            keepAlive.get("time").set(200L);
            keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());
            operationBuilder.addStep(op);

            execute(managementClient.getControllerClient(), operationBuilder.build());
        }

        static ModelNode execute(final ModelControllerClient client, final Operation op) throws IOException {
            final ModelNode result = client.execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail(Operations.getFailureDescription(result).toString());
            }
            return result;
        }

        static ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
            return execute(client, Operation.Factory.create(op));
        }
    }
}
