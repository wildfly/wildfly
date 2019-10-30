/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.batch.deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the start, stop and restart functionality for deployments.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentResourceTestCase extends AbstractBatchTestCase {

    private static final String DEPLOYMENT_NAME_1 = "test-batch-1.war";

    private static final String DEPLOYMENT_NAME_2 = "test-batch-2.war";

    @ArquillianResource
    // @OperateOnDeployment is required until WFARQ-13 is resolved
    @OperateOnDeployment(DEPLOYMENT_NAME_1)
    private ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT_NAME_1)
    public static WebArchive createDeployment1() {
        final Package pkg = DeploymentResourceTestCase.class.getPackage();
        final WebArchive deployment = createDefaultWar(DEPLOYMENT_NAME_1, pkg)
                .addClasses(CountingItemReader.class, CountingItemWriter.class);
        addJobXml(pkg, deployment, "test-chunk.xml");
        addJobXml(pkg, deployment, "test-chunk.xml", "same-test-chunk.xml");
        return deployment;
    }

    @Deployment(name = DEPLOYMENT_NAME_2)
    public static WebArchive createDeployment2() {
        final Package pkg = DeploymentResourceTestCase.class.getPackage();
        final WebArchive deployment = createDefaultWar(DEPLOYMENT_NAME_2, pkg)
                .addClasses(CountingItemReader.class, CountingItemWriter.class);
        addJobXml(pkg, deployment, "test-chunk.xml");
        addJobXml(pkg, deployment, "test-chunk.xml", "same-test-chunk.xml");
        addJobXml(pkg, deployment, "test-chunk-other.xml");
        addJobXml(deployment, EmptyAsset.INSTANCE, "invalid.xml");
        return deployment;
    }

    @Test
    public void testRootResourceJobXmlListing() throws Exception {
        // First deployment should only have two available XML descriptors
        validateJobXmlNames(DEPLOYMENT_NAME_1, "test-chunk.xml", "same-test-chunk.xml");
        // Second deployment should have 3 available descriptors and one missing descriptor as it's invalid
        validateJobXmlNames(DEPLOYMENT_NAME_2, Arrays.asList("test-chunk.xml", "same-test-chunk.xml", "test-chunk-other.xml"), Collections.singleton("invalid.xml"));
    }

    @Test
    public void testDeploymentJobXmlListing() throws Exception {
        // First deployment should have two available XML descriptors on the single job
        ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME_1, "subsystem", "batch-jberet", "job", "test-chunk");
        validateJobXmlNames(address, "test-chunk.xml", "same-test-chunk.xml");

        // Second deployment should have two available jobs. The first job should have two available XML descriptors the
        // second job should only have one descriptor.
        address = Operations.createAddress("deployment", DEPLOYMENT_NAME_2, "subsystem", "batch-jberet", "job", "test-chunk");
        validateJobXmlNames(address, "test-chunk.xml", "same-test-chunk.xml");
        address = Operations.createAddress("deployment", DEPLOYMENT_NAME_2, "subsystem", "batch-jberet", "job", "test-chunk-other");
        validateJobXmlNames(address, "test-chunk-other.xml");
    }

    @Test
    public void testEmptyResources() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME_2, "subsystem", "batch-jberet");
        final ModelNode op = Operations.createReadResourceOperation(address, true);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        final ModelNode result = executeOperation(op);
        final ModelNode otherJob = result.get("job", "test-chunk-other");
        Assert.assertTrue("Expected the test-chunk-other job resource to exist", otherJob.isDefined());
        Assert.assertEquals(0, otherJob.get("instance-count").asInt());
        Assert.assertEquals(0, otherJob.get("running-executions").asInt());
        Assert.assertFalse(otherJob.get("executions").isDefined());
    }

    private void validateJobXmlNames(final String deploymentName, final String... expectedDescriptors) throws IOException {
        validateJobXmlNames(deploymentName, Arrays.asList(expectedDescriptors), Collections.emptyList());
    }

    private void validateJobXmlNames(final ModelNode address, final String... expectedDescriptors) throws IOException {
        validateJobXmlNames(address, Arrays.asList(expectedDescriptors), Collections.emptyList());
    }

    private void validateJobXmlNames(final String deploymentName, final Collection<String> expectedDescriptors,
                                     final Collection<String> unexpectedDescriptors) throws IOException {
        final ModelNode address = Operations.createAddress("deployment", deploymentName, "subsystem", "batch-jberet");
        validateJobXmlNames(address, expectedDescriptors, unexpectedDescriptors);
    }

    private void validateJobXmlNames(final ModelNode address, final Collection<String> expectedDescriptors,
                                     final Collection<String> unexpectedDescriptors) throws IOException {
        final ModelNode op = Operations.createReadAttributeOperation(address, "job-xml-names");
        final ModelNode result = executeOperation(op);
        final Collection<String> jobNames = result.asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());

        Assert.assertEquals(expectedDescriptors.size(), jobNames.size());
        for (String xmlDescriptor : expectedDescriptors) {
            Assert.assertTrue(String.format("Expected %s to be in the list of job-xml-names.", xmlDescriptor),
                    jobNames.contains(xmlDescriptor));
        }

        for (String xmlDescriptor : unexpectedDescriptors) {
            Assert.assertFalse(String.format("Expected %s to NOT be in the list of job-xml-names.", xmlDescriptor),
                    jobNames.contains(xmlDescriptor));
        }
    }

    @SuppressWarnings("Duplicates")
    private ModelNode executeOperation(final ModelNode op) throws IOException {
        final ModelControllerClient client = managementClient.getControllerClient();
        final ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return Operations.readResult(result);
        }
        Assert.fail(Operations.getFailureDescription(result).asString());
        // Should never be reached
        return new ModelNode();
    }
}
