/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.jpa;

import java.net.URISyntaxException;
import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.ClusterDatabaseTestUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test of clustered Jakarta Persistence 2nd level cache implemented by Infinispan.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@ServerSetup(ClusteredJPA2LCTestCase.ServerSetupTask.class)
public class ClusteredJPA2LCTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = ClusteredJPA2LCTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ClusteredJPA2LCTestCase.class.getPackage());
        war.addAsWebInfResource(ClusteredJPA2LCTestCase.class.getPackage(), "persistence.xml",
                "classes/META-INF/persistence.xml");
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusterDatabaseTestUtil.startH2();
        IntermittentFailure.thisTestIsFailingIntermittently("https://issues.redhat.com/browse/WFLY-21506");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusterDatabaseTestUtil.stopH2();
    }

    // REST client to control entity creation, caching, eviction,... on the servers
    private Client restClient;

    @Before
    public void init() {
        this.restClient = ClientBuilder.newClient();
    }

    @After
    public void destroy() {
        this.restClient.close();
    }

    @Test
    public void testEntityCacheInvalidation(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL url0,
                                           @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL url1)
            throws Exception {
        final WebTarget node0 = getWebTarget(url0);
        final WebTarget node1 = getWebTarget(url1);

        final String entityId = "1";
        createEntity(node0, entityId);
        Assert.assertTrue(isInCache(node0, entityId));
        Thread.sleep(GRACE_TIME_TO_REPLICATE);
        addToCache(node1, entityId);
        Assert.assertTrue(isInCache(node1, entityId));
        evictFromCache(node1, entityId);
        Assert.assertFalse(isInCache(node0, entityId));
        Assert.assertFalse(isInCache(node1, entityId));
    }

    private boolean isInCache(WebTarget node, String entityId) {
        return Boolean.valueOf(
                node.path("isInCache").path(entityId).request().get().readEntity(String.class));
    }

    private void addToCache(WebTarget node, String entityId) {
        int status = node.path("cache").path(entityId).request().get().getStatus();
        Assert.assertEquals(204, status);
    }

    private void evictFromCache(WebTarget node, String entityId) {
        int status = node.path("evict").path(entityId).request().get().getStatus();
        Assert.assertEquals(204, status);
    }

    private void createEntity(WebTarget node, String entityId) {
        int status = node.path("create").path(entityId).request().get().getStatus();
        Assert.assertEquals(204, status);
    }

    protected WebTarget getWebTarget(URL url) throws URISyntaxException {
        return restClient.target(url.toURI());
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .add("/subsystem=datasources/data-source=h2:add(jndi-name=java:jboss/datasources/H2, enabled=true, use-java-context=true, connection-url=\"jdbc:h2:tcp://localhost:%s/MainPU;VARIABLE_BINARY=TRUE\", driver-name=h2)", DB_PORT)
                            .add("/subsystem=infinispan/cache-container=hibernate:write-attribute(name=marshaller, value=PROTOSTREAM)")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .add("/subsystem=infinispan/cache-container=hibernate:write-attribute(name=marshaller, value=JBOSS)")
                            .add("/subsystem=datasources/data-source=h2:remove")
                            .build())
                    .build()
                    );
        }
    }
}
