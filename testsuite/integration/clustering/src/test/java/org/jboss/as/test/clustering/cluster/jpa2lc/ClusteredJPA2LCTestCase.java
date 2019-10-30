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

package org.jboss.as.test.clustering.cluster.jpa2lc;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * Smoke test of clustered JPA 2nd level cache implemented by Infinispan.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class ClusteredJPA2LCTestCase {

    private static final String MODULE_NAME = ClusteredJPA2LCTestCase.class.getSimpleName();

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

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

    // management connection to node0
    private ModelControllerClient client0;

    // management connection to node1
    private ModelControllerClient client1;

    // REST client to control entity creation, caching, eviction,... on the servers
    private Client restClient = ClientBuilder.newClient();

    // /subsystem=infinispan/cache-container=hibernate/replicated-cache=entity-replicated
    static ModelNode CACHE_ADDRESS;

    static {
        CACHE_ADDRESS = new ModelNode();
        CACHE_ADDRESS.get("subsystem").set("infinispan");
        CACHE_ADDRESS.get("cache-container").set("hibernate");
        CACHE_ADDRESS.get("replicated-cache").set("entity-replicated");
    }

    @BeforeClass
    public static void ignore() {
        IntermittentFailure.thisTestIsFailingIntermittently("WFLY-10099");
    }

    @Test
    @InSequence(-1)
    public void setupCacheContainer() throws IOException {
        NodeUtil.start(controller, TWO_NODES);

        final ModelNode createEntityReplicatedCacheOp = new ModelNode();
        createEntityReplicatedCacheOp.get(ADDRESS).set(CACHE_ADDRESS);
        createEntityReplicatedCacheOp.get(OP).set(ADD);
        createEntityReplicatedCacheOp.get("mode").set("sync");

        client0 = createClient0();
        client1 = createClient1();

        final ModelNode result0 = client0.execute(createEntityReplicatedCacheOp);
        Assert.assertTrue(result0.toJSONString(false), result0.get(OUTCOME).asString().equals(SUCCESS));

        final ModelNode result1 = client1.execute(createEntityReplicatedCacheOp);
        Assert.assertTrue(result1.toJSONString(false), result1.get(OUTCOME).asString().equals(SUCCESS));

        NodeUtil.deploy(this.deployer, TWO_DEPLOYMENTS);
    }

    /**
     * We have a replicated entity cache between two nodes. Cache an entity on node0 and then verify that
     * node1 sees the cached entity as well. Then try evicting it (synchronously inside a JTA transaction)
     * on one node and see if it's been evicted on the other node as well. Finally, try caching it again.
     *
     * The two nodes don't actually have a shared database instance, but that doesn't matter for this test.
     */
    @Test
    @InSequence
    public void testEntityCacheReplication(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL url0,
                                           @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL url1)
            throws Exception {
        final WebTarget node0 = getWebTarget(url0);
        final WebTarget node1 = getWebTarget(url1);

        final String entityId = "1";
        createEntity(node0, entityId);
        Assert.assertTrue(isInCache(node0, entityId));
        Assert.assertTrue(isInCache(node1, entityId));
        evictFromCache(node1, entityId);
        Assert.assertFalse(isInCache(node0, entityId));
        Assert.assertFalse(isInCache(node1, entityId));
        addToCache(node0, entityId);
        Assert.assertTrue(isInCache(node0, entityId));
        Assert.assertTrue(isInCache(node1, entityId));
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

    @Test
    @InSequence(Integer.MAX_VALUE)
    public void tearDown() throws IOException {
        final ModelNode removeOp = new ModelNode();
        removeOp.get(ADDRESS).set(CACHE_ADDRESS);
        removeOp.get(OP).set(REMOVE_OPERATION);
        if (client0 != null) {
            client0.execute(removeOp);
            client0.close();
        }
        if (client1 != null) {
            client1.execute(removeOp);
            client1.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }


    protected WebTarget getWebTarget(URL url) throws URISyntaxException {
        return restClient.target(url.toURI());
    }

    protected static ModelControllerClient createClient0() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient1() throws UnknownHostException {
        return ModelControllerClient.Factory
                .create(InetAddress.getByName(TestSuiteEnvironment.getServerAddressNode1()),
                        TestSuiteEnvironment.getServerPort() + 100,
                        Authentication.getCallbackHandler());
    }
}
