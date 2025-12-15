/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.counter;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.infinispan.counter.api.Storage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.infinispan.counter.deployment.InfinispanCounterServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case to verify Infinispan counter module usage.
 * Verifies both VOLATILE and PERSISTENT timers.
 * The server setup configures a new cache container with org.infinispan.counter module and a default replicated cache.
 * Test creates a counter on one node, then tests counter availability on the other node.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(InfinispanCounterTestCase.ServerSetupTask.class)
public class InfinispanCounterTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = InfinispanCounterTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .addPackage(InfinispanCounterServlet.class.getPackage())
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan, org.infinispan.counter\n"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // WFLY-17968
                        new FilePermission("<<ALL FILES>>", "read,write"),
                        new RuntimePermission("getClassLoader")
                ), "permissions.xml")
                ;
    }

    @Test
    public void testVolatileCounters(@ArquillianResource(InfinispanCounterServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                                     @ArquillianResource(InfinispanCounterServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {
        this.test(Storage.VOLATILE, baseURL1, baseURL2);
    }

    @Test
    public void testPersistentCounters(@ArquillianResource(InfinispanCounterServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                                       @ArquillianResource(InfinispanCounterServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {
        this.test(Storage.PERSISTENT, baseURL1, baseURL2);
    }

    public void test(Storage storage, URL baseURL1, URL baseURL2) throws IOException, URISyntaxException {

        String counterName = storage.name() + UUID.randomUUID();

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCounterServlet.createURI(baseURL1, counterName, storage.name())))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(EntityUtils.toString(response.getEntity())));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCounterServlet.createURI(baseURL1, counterName)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(EntityUtils.toString(response.getEntity())));
            }

            // -> node2

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCounterServlet.createURI(baseURL2, counterName)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(3, Integer.parseInt(EntityUtils.toString(response.getEntity())));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCounterServlet.createURI(baseURL2, counterName)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(EntityUtils.toString(response.getEntity())));
            }

            // -> node1

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCounterServlet.createURI(baseURL1, counterName)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(5, Integer.parseInt(EntityUtils.toString(response.getEntity())));
            }
        }
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=infinispan/cache-container=counter:add(default-cache=repl, modules=[org.infinispan.counter])")
                            .add("/subsystem=infinispan/cache-container=counter/transport=jgroups:add")
                            .add("/subsystem=infinispan/cache-container=counter/replicated-cache=repl:add")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=infinispan/cache-container=counter:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }

}
