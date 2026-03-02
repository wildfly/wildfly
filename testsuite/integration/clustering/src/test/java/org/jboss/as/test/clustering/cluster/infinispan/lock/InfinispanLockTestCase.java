/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.lock;

import java.net.URL;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.infinispan.lock.deployment.InfinispanLockServlet;
import org.jboss.as.test.clustering.cluster.infinispan.lock.deployment.InfinispanLockServlet.LockOperation;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test case to verify Infinispan lock module usage.
 * The server setup configures a new cache container with org.infinispan.lock module and a default replicated cache.
 * Test creates a lock on one node, then tests lock availability on the other node.
 *
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(InfinispanLockTestCase.ServerSetupTask.class)
public class InfinispanLockTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = InfinispanLockTestCase.class.getSimpleName();

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
                .addPackage(InfinispanLockServlet.class.getPackage())
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan, org.infinispan.commons, org.infinispan.lock\n"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                ;
    }

    @Test
    void test(@ArquillianResource(InfinispanLockServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
              @ArquillianResource(InfinispanLockServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {

        String lockName = UUID.randomUUID().toString();

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.DEFINE)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.IS_LOCKED)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.LOCK)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.IS_LOCKED)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            // -> node2

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL2, lockName, LockOperation.IS_LOCKED)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            // -> node 1
            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.UNLOCK)));) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // unlock has no output – only check SC
            }

            // -> node 2

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL2, lockName, LockOperation.IS_LOCKED)));) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

            // -> node1

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanLockServlet.createURI(baseURL1, lockName, LockOperation.IS_LOCKED)));) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(Boolean.parseBoolean(EntityUtils.toString(response.getEntity())));
            }

        }
    }

    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=infinispan/cache-container=lock:add(default-cache=repl, modules=[org.infinispan.lock])")
                            .add("/subsystem=infinispan/cache-container=lock/transport=jgroups:add")
                            .add("/subsystem=infinispan/cache-container=lock/replicated-cache=repl:add")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=infinispan/cache-container=lock:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }

}
