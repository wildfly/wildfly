/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.modcluster;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.single.web.CommonJvmRoute;
import org.jboss.as.test.clustering.single.web.JvmRouteServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Worker failover test for modcluster. Creates basic scenario (one balancer and two workers) and sends a request when both
 * workers are alive, then it undeploys worker which responded and sends the request again.
 *
 * @author Matus Madzin
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WorkerFailoverTestCase {

    private static final String MODULE = "jvmroute";
    private static final String DEPLOYMENT_1 = "deployment-1";
    private static final String DEPLOYMENT_2 = "deployment-2";
    private static final String CONTAINER_1 = "con-1";
    private static final String CONTAINER_2 = "con-2";
    private static final String BALANCER = "balancer";
    private static final int TIMEOUT = 90;

    private static final Logger LOGGER = Logger.getLogger(WorkerFailoverTestCase.class);

    @ArquillianResource
    protected WildFlyContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    private String balancerConfigFile;
    private String worker1ConfigFile;
    private String worker2ConfigFile;
    private String undeployedApp;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        final String[] properties = CommonJvmRoute.getProperties();
        final PropertyPermission[] propertyPermissions = new PropertyPermission[properties.length];

        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE + ".war");
        war.addClasses(JvmRouteServlet.class, CommonJvmRoute.class);

        for (int i = 0; i < properties.length; i++) {
            propertyPermissions[i] = new PropertyPermission(properties[i], "read");
        }

        war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(propertyPermissions), "permissions.xml");

        return war;
    }

    @Before
    public void startAndSetupContainers() throws Exception {

        LOGGER.trace("*** starting balancer");
        controller.start(BALANCER);

        LOGGER.trace("*** will configure server now");
        balancerSetup();

        LOGGER.trace("*** starting worker1");
        controller.start(CONTAINER_1);

        LOGGER.trace("*** will configure worker1 now");
        workerSetup("node1", 10090);

        LOGGER.trace("*** starting worker2");
        controller.start(CONTAINER_2);

        LOGGER.trace("*** will configure worker2 now");
        workerSetup("node2", 10190);

        LOGGER.trace("*** will deploy " + DEPLOYMENT_1 + " and " + DEPLOYMENT_2);
        deployer.deploy(DEPLOYMENT_1);
        deployer.deploy(DEPLOYMENT_2);
    }

    /*
        Configures EAP on node "node0" as a load balancer (undertow filter).
     */
    private void balancerSetup() throws Exception {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(null, TestSuiteEnvironment.getServerAddress("node0"), 9990)) {

            /* Configuration backup */
            ModelNode op = createOpNode("path=jboss.server.config.dir", "read-attribute");
            op.get("name").set("path");
            ModelNode response = client.execute(op);
            Assert.assertEquals("Server's configuration file not found!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            balancerConfigFile = response.get("result").asString() + File.separator + System.getProperty("jboss.server.config.file.standalone", "standalone.xml");
            Files.copy(Paths.get(balancerConfigFile), Paths.get(balancerConfigFile + ".WorkerFailoverTestCase.backup"), REPLACE_EXISTING);

            /* Server configuration */
            op = createOpNode("subsystem=undertow/configuration=filter/mod-cluster=modcluster", "add");
            op.get("management-socket-binding").set("http");
            response = client.execute(op);
            Assert.assertEquals("Server's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            op = createOpNode("subsystem=undertow/server=default-server/host=default-host/filter-ref=modcluster", "add");
            response = client.execute(op);
            Assert.assertEquals("Server's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());
        }

    }

    /*
        Configures EAP as worker with proxies

        @param cliAddress - name of node where EAP will be configured e.g. node1.
        @param port - management port of the node.
     */
    private void workerSetup(String cliAddress, int port) throws Exception {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(null, TestSuiteEnvironment.getServerAddress(cliAddress), port)) {

            /* Configuration backup */
            ModelNode op = createOpNode("path=jboss.server.config.dir", "read-attribute");
            op.get("name").set("path");
            ModelNode response = client.execute(op);
            Assert.assertEquals("Workers's configuration file not found!\n" + response.toString(), SUCCESS, response.get("outcome").asString());


            String workerConfigFile = response.get("result").asString() + File.separator + System.getProperty("jboss.server.config.file.standalone-ha","/standalone-ha.xml");
            Files.copy(Paths.get(workerConfigFile), Paths.get(workerConfigFile + ".WorkerFailoverTestCase.backup"), REPLACE_EXISTING);

            if (port == 10090) worker1ConfigFile = workerConfigFile;
            else worker2ConfigFile = workerConfigFile;

            /* Server configuration */
            op = createOpNode("subsystem=modcluster/mod-cluster-config=configuration/", "write-attribute");
            op.get("name").set("advertise");
            op.get("value").set("false");
            response = client.execute(op);
            Assert.assertEquals("Worker's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            op = createOpNode("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1", "add");
            op.get("host").set(TestSuiteEnvironment.getServerAddress("node0"));
            op.get("port").set("8080");
            response = client.execute(op);
            Assert.assertEquals("Worker's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            op = createOpNode("subsystem=modcluster/mod-cluster-config=configuration", "list-add");
            op.get("name").set("proxies");
            op.get("value").set("proxy1");
            response = client.execute(op);
            Assert.assertEquals("Worker's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            op = createOpNode("subsystem=modcluster/mod-cluster-config=configuration", "write-attribute");
            op.get("name").set("status-interval");
            op.get("value").set("1");
            response = client.execute(op);
            Assert.assertEquals("Worker's configuration failed!\n" + response.toString(), SUCCESS, response.get("outcome").asString());

            ServerReload.executeReloadAndWaitForCompletion(client, ServerReload.TIMEOUT, false, TestSuiteEnvironment.getServerAddress(cliAddress), port);
        }
    }

    @After
    public void stopContainers() throws Exception {
        LOGGER.trace("*** undeploy applications");

        if (undeployedApp == null || !undeployedApp.equals(DEPLOYMENT_1)) deployer.undeploy(DEPLOYMENT_1);

        if (undeployedApp == null || !undeployedApp.equals(DEPLOYMENT_2)) deployer.undeploy(DEPLOYMENT_2);

        LOGGER.trace("*** stopping container");
        controller.stop(CONTAINER_1);
        controller.stop(CONTAINER_2);
        controller.stop(BALANCER);

        LOGGER.trace("*** reseting test configuration");
        Files.move(Paths.get(worker1ConfigFile + ".WorkerFailoverTestCase.backup"), Paths.get(worker1ConfigFile), REPLACE_EXISTING);
        Files.move(Paths.get(worker2ConfigFile + ".WorkerFailoverTestCase.backup"), Paths.get(worker2ConfigFile), REPLACE_EXISTING);
        Files.move(Paths.get(balancerConfigFile + ".WorkerFailoverTestCase.backup"), Paths.get(balancerConfigFile), REPLACE_EXISTING);
    }

    /*
        Checks that both workers are visible for balancer before the test.
     */
    private void infrastructureCheck() {
        /* Connects to the balancer */
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(null, TestSuiteEnvironment.getServerAddress("node0"), 9990)) {

            ModelNode checkWorker1 = null, checkWorker2 = null;

            /* Prepare opperations for retriving info about workers from the balancer*/
            try {
                checkWorker1 = createOpNode("subsystem=undertow/configuration=filter/mod-cluster=modcluster/balancer=mycluster/node=node-1", "read-children-names");
                checkWorker1.get("child-type").set("context");

                checkWorker2 = createOpNode("subsystem=undertow/configuration=filter/mod-cluster=modcluster/balancer=mycluster/node=node-2", "read-children-names");
                checkWorker2.get("child-type").set("context");
            } catch (Exception e) {
                LOGGER.error("Not able to create OpNodes", e);
                Assert.fail("Not able to create OpNodes. For more information look into log file.");
            }

            int iteration = 0;
            ModelNode response1 = null, response2 = null;
            boolean result1 = false, result2 = false;

            /* Periodically checks whether both workers are registered to the balancer in TIMEOUT */
            do {
                try {
                    response1 = client.execute(checkWorker1);
                    response2 = client.execute(checkWorker2);
                    iteration++;

                    result1 = response1.get("result").asString().contains("jvmroute");
                    result2 = response2.get("result").asString().contains("jvmroute");

                    if (result1 && result2) break;

                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    LOGGER.error("Not able to execute OpNode and check the result", e);
                    Assert.fail("Not able to execute OpNode and check the result. For more information look into log file.");
                }
            } while (iteration < TIMEOUT);

            /* In case any worker is not ready (after timeout) then test fails */
            Assert.assertTrue("TIMEOUT ELAPSED: Balancer is not able to see both workers!\n", result1);
            Assert.assertTrue("TIMEOUT ELAPSED: Balancer is not able to see both workers!\n", result2);

        } catch (IOException e) {
            LOGGER.error("InfrastructureCheck failed", e);
            Assert.fail("IOException: For more information look into log file.");
        }
    }

    @Test
    public void workerFailoverTest() throws URISyntaxException, IOException {
        String address = TestSuiteEnvironment.getServerAddress("node0");
        URL url = new URL("http", address, 8080, "/jvmroute/jvmroute");
        URI uri = url.toURI();

        /* Checks whether all servers are set as expected */
        infrastructureCheck();

        try (CloseableHttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = null;
            String worker1, worker2;

            try {
                /* The first HTTP request */
                response = httpClient.execute(new HttpGet(uri));

                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                HttpEntity entity = response.getEntity();
                Assert.assertNotNull(entity);
                worker1 = EntityUtils.toString(entity);

                if (!worker1.equals("worker-1")) Assert.assertEquals("worker-2", worker1);

                /* Undeploy worker which responsed the first HTTP request */
                if (worker1.equals("worker-1")) {
                    NodeUtil.undeploy(this.deployer, DEPLOYMENT_1);
                    undeployedApp = DEPLOYMENT_1;
                } else {
                    NodeUtil.undeploy(this.deployer, DEPLOYMENT_2);
                    undeployedApp = DEPLOYMENT_2;
                }

                /* The second HTTP request */
                response = httpClient.execute(new HttpGet(uri));

                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                entity = response.getEntity();
                Assert.assertNotNull(entity);
                worker2 = EntityUtils.toString(entity);

                /* Checks that the second node responded the second request */
                if (worker1.equals("worker-1")) Assert.assertEquals("worker-2", worker2);
                else Assert.assertEquals("worker-1", worker2);

            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
