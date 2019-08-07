/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.jpa2lcCustomRegion;

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
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_2;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.TWO_DEPLOYMENTS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.TWO_NODES;

/**
 * Test of clustered JPA 2nd level cache implemented by Infinispan using entity custom region.
 * <p>
 * In persistence.xml we add the following:
 * <code>
 * <property name="hibernate.cache.infinispan.entity.cfg" value="entity-replicated"/>
 * </code>
 * This has the effect of using the Infinispan cache "entity-replicated" as the template for entities's caches;
 * </p>
 * <p>
 * In the standalone-ha.xml, we have an invalidation-cache named "entity" in the cache container "hibernate" of
 * the Infinispan subsystem;
 * By default, this cache would be used as the template for entities's caches;
 * Here we are overriding this default creating a replicated-cache in the cache container "hibernate", and using it
 * as template;
 * </p>
 * <p>
 * We are using the entity {@link DummyEntityCustomRegion}, which also defines a custom region name to be used for
 * its second level cache name:
 * <code>
 *
 * @author Jan Martiska and Tommaso Borgato
 * @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME)
 * </code>
 * </p>
 * <p>
 * The result is that, for the entity {@link DummyEntityCustomRegion}, a replicated-cache named
 * "ClusteredJPA2LCCustomRegionTestCase.war#MainPU.DUMMY_ENTITY_REGION_NAME" is created;
 * </p>
 * <p>
 * The goals of this test are:
 * <ul>
 * <li>verify that the cache created for the entity {@link DummyEntityCustomRegion} is actually a replicated-cache</li>
 * <li>verify that the cache created for the entity {@link DummyEntityCustomRegion} is actually named accordingly with the annotation on the entity</li>
 * </ul>
 * </p>
 */
@RunWith(Arquillian.class)
public class ClusteredJPA2LCCustomRegionTestCase {

    private static final String MODULE_NAME = ClusteredJPA2LCCustomRegionTestCase.class.getSimpleName();

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

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
        war.addPackage(ClusteredJPA2LCCustomRegionTestCase.class.getPackage());
        war.addAsWebInfResource(ClusteredJPA2LCCustomRegionTestCase.class.getPackage(), "persistence.xml",
                "classes/META-INF/persistence.xml");
        war.addAsWebInfResource(getWebXml(), "web.xml");
        war.setManifest(new StringAsset(
                Descriptors.create(ManifestDescriptor.class)
                        .attribute("Dependencies", "org.infinispan export,org.infinispan.commons export")
                        .exportAsString()));
        return war;
    }

    /**
     * We need a reference to the <code>hibernate</code> cache-container to access its properties programmatically
     */
    private static StringAsset getWebXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<web-app version=\"3.0\" metadata-complete=\"false\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">\n"
                + "    <resource-ref>\n"
                + "       <res-ref-name>infinispan/hibernate</res-ref-name>\n"
                + "       <res-type>org.infinispan.manager.CacheContainer</res-type>"
                + "       <lookup-name>java:jboss/infinispan/container/hibernate</lookup-name>\n"
                + "    </resource-ref>\n"
                + "</web-app>");
    }

    // management connection to node0
    private ModelControllerClient client0;

    // management connection to node1
    private ModelControllerClient client1;

    // REST client to control entity creation, caching, eviction,... on the servers
    private Client restClient = ClientBuilder.newClient();

    // /subsystem=infinispan/cache-container=hibernate/replicated-cache=entity-replicated
    private static ModelNode CACHE_ADDRESS;

    static {
        CACHE_ADDRESS = new ModelNode();
        CACHE_ADDRESS.get("subsystem").set("infinispan");
        CACHE_ADDRESS.get("cache-container").set("hibernate");
        CACHE_ADDRESS.get("replicated-cache").set("entity-replicated-template");
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
        Assert.assertEquals(result0.toJSONString(false), result0.get(OUTCOME).asString(), SUCCESS);

        final ModelNode result1 = client1.execute(createEntityReplicatedCacheOp);
        Assert.assertEquals(result1.toJSONString(false), result1.get(OUTCOME).asString(), SUCCESS);

        NodeUtil.deploy(this.deployer, TWO_DEPLOYMENTS);
    }

    /**
     * We have a replicated entity cache between two nodes.
     * We verify that the cache is actually a replicated cache and that it was named accordingly to the
     * {@link DummyEntityCustomRegion} entity annotation
     * <code>@Cache(region = DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME)</code>;
     * <p>
     * The two nodes don't actually have a shared database instance, but that doesn't matter for this test.
     */
    @Test
    @InSequence(1)
    public void testEntityInCustomCache(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL url0,
                                        @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL url1)
            throws Exception {
        final WebTarget node0 = getWebTarget(url0);
        final WebTarget node1 = getWebTarget(url1);

        final String entityId = "2";

        // create entity
        createEntity(node0, entityId);

        // put entity in second level cache
        addEntityToCache(node0, entityId);

        // get the name of the cache containing the entity
        String regionName = getRegionNameForEntity(node1, entityId);
        Assert.assertNotNull(
                String.format("Region name for entity '%s' should NOT be null!", DummyEntityCustomRegion.class.getCanonicalName()),
                regionName);
        Assert.assertTrue(
                String.format("Region name for entity '%s' should be something like %s.war#MainPUCustomRegion.%s",
                        DummyEntityCustomRegion.class.getCanonicalName(),
                        ClusteredJPA2LCCustomRegionTestCase.class.getName(),
                        DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME),
                regionName.contains(DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME));

        // verify that cache is actually a replicated one
        Boolean isReplicated = getCustomRegionCacheIsReplicated(node1, regionName);
        Assert.assertTrue(String.format("Cache '%s' should be a replicated-cache!", regionName), isReplicated);
        Boolean isInvalidation = getCustomRegionCacheIsInvalidation(node0, regionName);
        Assert.assertFalse(String.format("Cache '%s' should NOT be an invalidation-cache!", regionName), isInvalidation);
    }

    /**
     * We have a replicated entity cache between two nodes.
     * We verify that the cache is actually a replicated cache and that it was configured accordingly to the
     * settings in persistence.xml;
     * <p>
     * The two nodes don't actually have a shared database instance, but that doesn't matter for this test.
     */
    @Test
    @InSequence(1)
    public void testPropsInCustomCache(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL url0,
                                       @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL url1)
            throws Exception {
        final WebTarget node0 = getWebTarget(url0);
        final WebTarget node1 = getWebTarget(url1);

        final String entityId = "3";

        // create entity
        createEntity(node0, entityId);

        // put entity in second level cache
        addEntityToCache(node0, entityId);

        // get the name of the cache containing the entity
        String regionName = getRegionNameForEntity(node1, entityId);
        Assert.assertNotNull(
                String.format("Region name for entity '%s' should NOT be null!", DummyEntityCustomRegion.class.getCanonicalName()),
                regionName);

        // max_entries
        Long evictionMaxEntries = getEvictionMaxEntries(node0, regionName);
        Assert.assertEquals(String.format("Cache '%s' should have attribute memory.size=99991", regionName), evictionMaxEntries.longValue(), 99991);

        // lifespan
        Long expirationLifespan = getExpirationLifespan(node0, regionName);
        Assert.assertEquals(String.format("Cache '%s' should have attribute expiration.lifespan=99992", regionName), expirationLifespan.longValue(), 99992);

        // max_idle
        Long expirationMaxIdle = getExpirationMaxIdle(node0, regionName);
        Assert.assertEquals(String.format("Cache '%s' should have attribute expiration.maxIdle=99993", regionName), expirationMaxIdle.longValue(), 99993);

        // wake_up_interval
        Long expirationWakeUpInterval = getExpirationWakeUpInterval(node0, regionName);
        Assert.assertEquals(String.format("Cache '%s' should have attribute expiration.wakeUpInterval=9994", regionName), expirationWakeUpInterval.longValue(), 9994);
    }

    private void createEntity(WebTarget node, String entityId) {
        int status = node.path("custom-region").path("create").path(entityId).request().get().getStatus();
        Assert.assertEquals(204, status);
    }

    private void addEntityToCache(WebTarget node, String entityId) {
        int status = node.path("custom-region").path("cache").path(entityId).request().get().getStatus();
        Assert.assertEquals(204, status);
    }

    private String getRegionNameForEntity(WebTarget node, String entityId) {
        Response response = node.path("custom-region").path("region-name")
                .queryParam("name", DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME)
                .queryParam("id", entityId)
                .request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return response.readEntity(String.class);
    }

    private Boolean getCustomRegionCacheIsReplicated(WebTarget node, String regionName) {
        Response response = node.path("custom-region").path("is-replicated").path(regionName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Boolean.class));
    }

    private Boolean getCustomRegionCacheIsInvalidation(WebTarget node, String regionName) {
        Response response = node.path("custom-region").path("is-invalidation").path(regionName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Boolean.class));
    }

    private Long getEvictionMaxEntries(WebTarget node, String cacheName) {
        Response response = node.path("custom-region").path("eviction-max-entries").path(cacheName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Long.class));
    }

    private Long getExpirationLifespan(WebTarget node, String cacheName) {
        Response response = node.path("custom-region").path("expiration-lifespan").path(cacheName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Long.class));
    }

    private Long getExpirationMaxIdle(WebTarget node, String cacheName) {
        Response response = node.path("custom-region").path("expiration-max-idle").path(cacheName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Long.class));
    }

    private Long getExpirationWakeUpInterval(WebTarget node, String cacheName) {
        Response response = node.path("custom-region").path("expiration-wake-up-interval").path(cacheName).request().get();
        int status = response.getStatus();
        Assert.assertEquals(200, status);
        return (response.readEntity(Long.class));
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
