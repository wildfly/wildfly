/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc. and individual contributors
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
package org.jboss.as.test.integration.management.deploy.runtime;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloStatisticsApplication;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloBadResource;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.PureProxyApiService;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.PureProxyEndPoint;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.SubHelloResource;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloSometimesBadResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsMethodStatisticsTestCase extends AbstractRuntimeTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private static final String DEPLOYMENT_NAME = "hello-stats.war";
    private static final String REST_RESOURCE_PATHS = "rest-resource-paths";
    private static final String SUB_RESOURCE_LOCATORS = "sub-resource-locators";
    private static final String REST_RESOURCE = "rest-resource";
    private static final String RESET_STATISTICS = "reset-statistics";
    private static final String METHOD_STATISTICS = "method_statistics";
    private static final ModelControllerClient controllerClient =
            TestSuiteEnvironment.getModelControllerClient();

    private static final String SUBSYSTEM_NAME = "jaxrs";
    private static final PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClass(HelloStatisticsApplication.class);
        war.addClass(HelloResource.class);
        war.addClass(SubHelloResource.class);
        war.addClass(PureProxyApiService.class);
        war.addClass(PureProxyEndPoint.class);
        war.addClass(HelloBadResource.class);
        war.addClass(HelloSometimesBadResource.class);
        return war;
    }

    @After
    public void after() throws Exception {
        // The server is not restarted between tests.
        // Return attribute to default state, for next test
        ModelNode attResult = setStatisticsFlag (false);
    }

    @Test
    public void testStatisticsEnabled() throws Exception {

        ModelNode statisticsNode = setupRunningScenario();
        assertTrue(statisticsNode.get("invocation-count").get(0).asLong() == 2);
        assertTrue(statisticsNode.get("failure-count").get(0).asLong() == 0);
        assertTrue(statisticsNode.get("average-execution-time").get(0).asLong() > 0);
        assertTrue(statisticsNode.get("total-execution-time").get(0).asLong() > 0);
    }

    @Test
    public void testReadWriteStatisticsAttribute() throws Exception {

        // check default value
        ModelNode readAtt = Util.createOperation(READ_ATTRIBUTE_OPERATION, subsystemAddress);
        readAtt.get("name").set(STATISTICS_ENABLED);

        ModelNode readResult = controllerClient.execute(readAtt);
        assertFalse(readResult.get("result").asBoolean());

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        // check for change
        ModelNode chkResult = controllerClient.execute(readAtt);
        assertTrue(chkResult.get("result").asBoolean());
    }

    @Test
    public void testFailureCnt() throws Exception {

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        try {
            performCall("hello/bad/call");

        } catch (Exception e) {
            // get the resource data
            ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION,
                    PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                            .append(SUBSYSTEM, SUBSYSTEM_NAME)
                            .append(REST_RESOURCE, HelloBadResource.class.getCanonicalName()));
            readResource.get(INCLUDE_RUNTIME).set(true);
            ModelNode result = controllerClient.execute(readResource);
            assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

            ModelNode res = Operations.readResult(result);
            assertThat(res.isDefined(), is(true));

            List<ModelNode> subResList = res.get(REST_RESOURCE_PATHS).asList();
            assertThat(subResList.size(), is(1));

            ModelNode statisticsNode = subResList.get(0).get(METHOD_STATISTICS);
            assertTrue(statisticsNode.get("failure-count").get(0).asLong() == 1);
        }
    }

    @Test
    public void testSometimesBad() throws Exception {

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        performCall("hello/sometimes/bad");
        // get the resource data
        ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME)
                        .append(REST_RESOURCE, HelloSometimesBadResource.class.getCanonicalName()));
        readResource.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        List<ModelNode> subResList = res.get(REST_RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(1));

        ModelNode statisticsNode = subResList.get(0).get(METHOD_STATISTICS);
        assertTrue(statisticsNode.get("invocation-count").get(0).asLong() == 1);
        assertTrue(statisticsNode.get("failure-count").get(0).asLong() == 0);

        try {
            performCall("hello/sometimes/bad");

        } catch (Exception e) {
            // get the resource data
            ModelNode readResourceA = Util.createOperation(READ_RESOURCE_OPERATION,
                    PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                            .append(SUBSYSTEM, SUBSYSTEM_NAME)
                            .append(REST_RESOURCE, HelloSometimesBadResource.class.getCanonicalName()));
            readResourceA.get(INCLUDE_RUNTIME).set(true);
            ModelNode resultA = controllerClient.execute(readResourceA);
            assertThat("Failed to list resources: " + resultA,
                    Operations.isSuccessfulOutcome(resultA), is(true));

            ModelNode resA = Operations.readResult(resultA);
            assertThat(resA.isDefined(), is(true));

            List<ModelNode> subResListA = resA.get(REST_RESOURCE_PATHS).asList();
            assertThat(subResListA.size(), is(1));

            ModelNode statisticsNodeA = subResListA.get(0).get(METHOD_STATISTICS);
            assertTrue(statisticsNodeA.get("failure-count").get(0).asLong() == 1);
            assertTrue(statisticsNodeA.get("invocation-count").get(0).asLong() == 2);
        }
    }

    @Test
    public void testMultipleEndpoints() throws Exception {

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        // get the resource data
        ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME)
                        .append(REST_RESOURCE, HelloResource.class.getCanonicalName()));
        readResource.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        List<ModelNode> subResList = res.get(REST_RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(4));

        ModelNode statisticsNode = subResList.get(0).get(METHOD_STATISTICS);
        assertTrue(statisticsNode.get("invocation-count").get(0).asLong() == 0);

        List<ModelNode> locSubResList = res.get(SUB_RESOURCE_LOCATORS).asList();
        assertThat(locSubResList.size(), is(1));

        List<ModelNode> locSubRes = locSubResList.get(0).get(REST_RESOURCE_PATHS).asList();
        assertThat(locSubRes.size(), is(2));

        ModelNode locStatisticsNode = locSubRes.get(0).get(METHOD_STATISTICS);
        assertTrue(locStatisticsNode.get("invocation-count").get(0).asLong() == 0);
    }

    @Test
    public void testSubsystemList() throws Exception {

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        // get the resource data
        ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME));
        readResource.get(INCLUDE_RUNTIME).set(true);
        readResource.get(RECURSIVE).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        List<ModelNode> resList = res.get(REST_RESOURCE).asList();
        assertThat(resList.size(), is(4));

        ModelNode mNode = resList.get(0);
        String[] keys = mNode.keys().toArray(new String[mNode.keys().size()]);
        ModelNode rootNode = resList.get(0).get(keys[0]);
        ModelNode subResPathNode = rootNode.get(REST_RESOURCE_PATHS);
        assertTrue(subResPathNode.toString().contains(METHOD_STATISTICS));
    }

    @Test
    public void testReset() throws Exception {

        ModelNode statisticsNode = setupRunningScenario();
        assertTrue(statisticsNode.get("invocation-count").get(0).asLong() == 2);

        // reset stats
        ModelNode statisticRest = Util.createOperation(RESET_STATISTICS, subsystemAddress);
        ModelNode restResult = controllerClient.execute(statisticRest);
        assertTrue(Operations.isSuccessfulOutcome(restResult));

        // get the resource data
        ModelNode readResource =  Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME)
                        .append(REST_RESOURCE, PureProxyEndPoint.class.getCanonicalName()));
        readResource.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        List<ModelNode> subResList = res.get(REST_RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(1));

        ModelNode statNode = subResList.get(0).get(METHOD_STATISTICS);
        assertTrue(statNode.get("invocation-count").get(0).asLong() == 0);

        // call endpoint again and confirm count works
        assertThat(performCall("hello/pure/proxy/test/Hello/World"), is("Hello World"));

        // get the resource data
        ModelNode readResourceA =  Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME)
                        .append(REST_RESOURCE, PureProxyEndPoint.class.getCanonicalName()));
        readResourceA.get(INCLUDE_RUNTIME).set(true);
        ModelNode resultA = controllerClient.execute(readResourceA);
        assertThat("Failed to list resources: " + result,
                Operations.isSuccessfulOutcome(resultA), is(true));

        ModelNode resA = Operations.readResult(resultA);
        assertThat(resA.isDefined(), is(true));

        List<ModelNode> subResListA = resA.get(REST_RESOURCE_PATHS).asList();
        assertThat(subResListA.size(), is(1));

        ModelNode statisticsNodeA = subResListA.get(0).get(METHOD_STATISTICS);
        assertTrue(statisticsNodeA.get("invocation-count").get(0).asLong() == 1);
    }

    private ModelNode setStatisticsFlag (boolean flag) throws Exception {
        ModelNode writeAtt = Util.createOperation(WRITE_ATTRIBUTE_OPERATION,
                PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME));
        writeAtt.get("name").set(STATISTICS_ENABLED);
        writeAtt.get("value").set(flag);
        return controllerClient.execute(writeAtt);
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    private ModelNode setupRunningScenario() throws Exception {

        ModelNode writeResult = setStatisticsFlag (true);
        assertTrue(Operations.isSuccessfulOutcome(writeResult));

        // made two calls for counting
        assertThat(performCall("hello/pure/proxy/test/Hello/World"), is("Hello World"));
        Thread.sleep(100);
        performCall("hello/pure/proxy/test/Hello/World");

        // get the resource data
        ModelNode readResource =  Util.createOperation(READ_RESOURCE_OPERATION,
                PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                        .append(SUBSYSTEM, SUBSYSTEM_NAME)
                        .append(REST_RESOURCE, PureProxyEndPoint.class.getCanonicalName()));
        readResource.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        List<ModelNode> subResList = res.get(REST_RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(1));

        ModelNode statisticsNode = subResList.get(0).get(METHOD_STATISTICS);

        return statisticsNode;
    }
}
