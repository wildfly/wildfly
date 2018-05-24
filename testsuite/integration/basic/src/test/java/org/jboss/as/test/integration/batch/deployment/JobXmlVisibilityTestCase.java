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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.BatchExecutionService;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JobXmlVisibilityTestCase extends AbstractBatchTestCase {
    private static final String EAR = "test-visibility.ear";
    private static final String EAR_ISOLATED = "test-visibility-isolated.ear";
    private static final String EAR_WITH_LIB = "test-with-lib.ear";
    private static final String EAR_WITH_LIB_ISOLATED = "test-isolated-with-lib.ear";
    private static final String WAR_WITH_LIB = "test-war-with-lib.war";

    @ArquillianResource
    // @OperateOnDeployment is required until WFARQ-13 is resolved, any deployment should suffice though
    @OperateOnDeployment(EAR)
    private ManagementClient managementClient;

    @Deployment(name = EAR)
    public static EnterpriseArchive visibleEarDeployment() {
        return createEar(EAR, "visible");
    }

    @Deployment(name = EAR_ISOLATED)
    public static EnterpriseArchive isolatedEarDeployment() {
        return createEar(EAR_ISOLATED, "isolated")
                .addAsManifestResource(new StringAsset(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<jboss-deployment-structure>\n" +
                                "    <ear-subdeployments-isolated>true</ear-subdeployments-isolated>\n" +
                                "</jboss-deployment-structure>"
                ), "jboss-deployment-structure.xml");
    }

    @Deployment(name = EAR_WITH_LIB)
    public static EnterpriseArchive earWithLibDeployment() {
        return createEar(EAR_WITH_LIB, "with-lib")
                .addAsLibrary(createJar("lib-in-ear.jar"));
    }

    @Deployment(name = EAR_WITH_LIB_ISOLATED)
    public static EnterpriseArchive isolatedEarWithLibDeployment() {
        return createEar(EAR_WITH_LIB_ISOLATED, "isolated-with-lib")
                .addAsLibrary(createJar("lib-in-ear.jar"))
                .addAsManifestResource(new StringAsset(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<jboss-deployment-structure>\n" +
                                "    <ear-subdeployments-isolated>true</ear-subdeployments-isolated>\n" +
                                "</jboss-deployment-structure>"
                ), "jboss-deployment-structure.xml");
    }

    @Deployment(name = WAR_WITH_LIB)
    public static WebArchive warWithLibDeployment() {
        return createWar(WAR_WITH_LIB)
                .addClasses(CountingItemReader.class, CountingItemWriter.class)
                .addAsLibrary(createJar("lib-in-war.jar"));
    }

    /**
     * Tests that all job XML files are visible from the WAR.
     */
    @Test
    public void testJobXmlIsVisible() throws Exception {
        validateJobXmlNames(deploymentAddress(EAR, "war-in-ear-visible.war"), Arrays.asList("test-war.xml", "test-ejb.xml"));
    }

    /**
     * Tests that the WAR can only see the job XML from the WAR and the EJB can only see the job XML from the EJB.
     */
    @Test
    public void testJobXmlIsIsolated() throws Exception {
        validateJobXmlNames(deploymentAddress(EAR_ISOLATED, "war-in-ear-isolated.war"), Collections.singleton("test-war.xml"));

        validateJobXmlNames(deploymentAddress(EAR_ISOLATED, "ejb-in-ear-isolated.jar"), Collections.singleton("test-ejb.xml"));
    }

    /**
     * Tests that the WAR can see the job XML from the WAR itself, the EJB and the EAR's global dependency. The EJB
     * should be able to see the job XML from the EJB itself and the EAR's global dependency.
     */
    @Test
    public void testJobXmlIsVisibleJar() throws Exception {
        validateJobXmlNames(deploymentAddress(EAR_WITH_LIB, "war-in-ear-with-lib.war"), Arrays.asList("test-war.xml", "test-ejb.xml", "test-jar.xml"));

        validateJobXmlNames(deploymentAddress(EAR_WITH_LIB, "ejb-in-ear-with-lib.jar"), Arrays.asList("test-ejb.xml", "test-jar.xml"));
    }

    /**
     * Tests that the WAR can see the job XML from the WAR itself and the EAR's global dependency. The EJB
     * should be able to see the job XML from the EJB itself and the EAR's global dependency.
     */
    @Test
    public void testJobXmlIsIsolatedJar() throws Exception {
        validateJobXmlNames(deploymentAddress(EAR_WITH_LIB_ISOLATED, "war-in-ear-isolated-with-lib.war"), Arrays.asList("test-war.xml", "test-jar.xml"));

        validateJobXmlNames(deploymentAddress(EAR_WITH_LIB_ISOLATED, "ejb-in-ear-isolated-with-lib.jar"), Arrays.asList("test-ejb.xml", "test-jar.xml"));
    }

    /**
     * Test that a WAR will see the job XML from a direct dependency.
     */
    @Test
    public void testJobXmlInWar() throws Exception {
        validateJobXmlNames(deploymentAddress(WAR_WITH_LIB, null), Arrays.asList("test-war.xml", "test-jar.xml"));
    }

    private void validateJobXmlNames(final ModelNode address, final Collection<String> expected) throws IOException {
        Set<String> jobXmlNames = getJobXmlNames(address);
        Assert.assertEquals(expected.size(), jobXmlNames.size());
        Assert.assertTrue("Expected the following job XML names: " + expected, jobXmlNames.containsAll(expected));
        final Collection<String> expectedJobNames = new LinkedHashSet<>();
        for (String jobXmlName : expected) {
            final int end = jobXmlName.indexOf(".xml");
            expectedJobNames.add(jobXmlName.substring(0, end));
        }
        final Set<String> jobNames = getJobNames(address);
        Assert.assertEquals(expectedJobNames.size(), jobNames.size());
        Assert.assertTrue("Expected the following job names: " + expectedJobNames, jobNames.containsAll(expectedJobNames));
    }

    private Set<String> getJobXmlNames(final ModelNode address) throws IOException {
        final ModelNode op = Operations.createReadAttributeOperation(address, "job-xml-names");
        final ModelNode result = executeOperation(op);
        return result.asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
    }

    private Set<String> getJobNames(final ModelNode address) throws IOException {
        final ModelNode op = Operations.createOperation(ClientConstants.READ_CHILDREN_NAMES_OPERATION, address);
        op.get(ClientConstants.CHILD_TYPE).set("job");
        final ModelNode result = executeOperation(op);
        return result.asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
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

    private static EnterpriseArchive createEar(final String name, final String suffix) {
        return ShrinkWrap.create(EnterpriseArchive.class, name)
                .addAsModule(createEjb("ejb-in-ear-" + suffix + ".jar"))
                .addAsModule(createWar("war-in-ear-" + suffix + ".war"));
    }

    private static WebArchive createWar(final String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        addJobXml(war, "test-war");
        return war;
    }

    private static JavaArchive createEjb(final String name) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, name)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                // Add at least one EJB annotated class to make this an EJB JAR
                .addClasses(CountingItemReader.class, CountingItemWriter.class, BatchExecutionService.class);
        addJobXml(jar, "test-ejb");
        return jar;
    }

    private static JavaArchive createJar(final String name) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, name)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(AbstractBatchTestCase.class);
        addJobXml(jar, "test-jar");
        return jar;
    }

    private static ModelNode deploymentAddress(final String deploymentName, final String subDeploymentName) {
        if (subDeploymentName == null) {
            return Operations.createAddress(
                    "deployment",
                    deploymentName,
                    "subsystem",
                    "batch-jberet"
            );
        }
        return Operations.createAddress(
                "deployment",
                deploymentName,
                "subdeployment",
                subDeploymentName,
                "subsystem",
                "batch-jberet"
        );
    }
}
