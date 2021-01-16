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

package org.jboss.as.test.clustering.cluster.singleton;

import static org.jboss.as.test.clustering.ClusterTestUtil.execute;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.servlet.TraceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(SingletonDeploymentTestCase.ServerSetupTask.class)
public abstract class SingletonDeploymentTestCase extends AbstractClusteringTestCase {

    static final String SINGLETON_DEPLOYMENT_1 = "singleton-deployment-1";
    static final String SINGLETON_DEPLOYMENT_2 = "singleton-deployment-2";

    private static final String MODULE_NAME = SingletonDeploymentTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deploymentHelper1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deploymentHelper2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addPackage(TraceServlet.class.getPackage());
        return war;
    }

    public static final int DELAY = TimeoutUtil.adjust(5000);

    private final String moduleName;
    private final String deploymentName;

    SingletonDeploymentTestCase(String moduleName, String deploymentName) {
        this.moduleName = moduleName;
        this.deploymentName = deploymentName;
    }

    @Test
    public void test(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2,
            @ArquillianResource(TraceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(TraceServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {

        this.deploy(SINGLETON_DEPLOYMENT_1);
        Thread.sleep(DELAY);
        this.deploy(SINGLETON_DEPLOYMENT_2);
        Thread.sleep(DELAY);

        String primaryProviderRequest = String.format("/subsystem=singleton/singleton-policy=default/deployment=%s:read-attribute(name=primary-provider)", this.deploymentName);
        String isPrimaryRequest = String.format("/subsystem=singleton/singleton-policy=default/deployment=%s:read-attribute(name=is-primary)", this.deploymentName);
        String getProvidersRequest = String.format("/subsystem=singleton/singleton-policy=default/deployment=%s:read-attribute(name=providers)", this.deploymentName);

        Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
        Assert.assertEquals(NODE_1, execute(client2, primaryProviderRequest).asStringOrNull());
        Assert.assertFalse(execute(client2, isPrimaryRequest).asBoolean(true));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));

        URI uri1 = TraceServlet.createURI(new URL(baseURL1.getProtocol(), baseURL1.getHost(), baseURL1.getPort(), "/" + this.moduleName + "/"));
        URI uri2 = TraceServlet.createURI(new URL(baseURL2.getProtocol(), baseURL2.getHost(), baseURL2.getPort(), "/" + this.moduleName + "/"));

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(SINGLETON_DEPLOYMENT_1);

            Thread.sleep(DELAY);

            Assert.assertEquals(NODE_2, execute(client2, primaryProviderRequest).asStringOrNull());
            Assert.assertTrue(execute(client2, isPrimaryRequest).asBoolean(false));
            Assert.assertEquals(Collections.singletonList(NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).collect(Collectors.toList()));

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(SINGLETON_DEPLOYMENT_1);

            Thread.sleep(DELAY);

            Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
            Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
            Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
            Assert.assertEquals(NODE_1, execute(client2, primaryProviderRequest).asStringOrNull());
            Assert.assertFalse(execute(client2, isPrimaryRequest).asBoolean(true));
            Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.undeploy(SINGLETON_DEPLOYMENT_2);

            Thread.sleep(DELAY);

            Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
            Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
            Assert.assertEquals(Collections.singletonList(NODE_1), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).collect(Collectors.toList()));

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.deploy(SINGLETON_DEPLOYMENT_2);

            Thread.sleep(DELAY);

            Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
            Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
            Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
            Assert.assertEquals(NODE_1, execute(client2, primaryProviderRequest).asStringOrNull());
            Assert.assertFalse(execute(client2, isPrimaryRequest).asBoolean(true));
            Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            this.undeploy(SINGLETON_DEPLOYMENT_1);
            Thread.sleep(DELAY);
            this.undeploy(SINGLETON_DEPLOYMENT_2);
            Thread.sleep(DELAY);
        }
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        ServerSetupTask() {
            this.builder.node(TWO_NODES)
                    .setup("/subsystem=singleton/singleton-policy=default/election-policy=simple:write-attribute(name=name-preferences,value=%s)", Arrays.toString(TWO_NODES))
                    .teardown("/subsystem=singleton/singleton-policy=default/election-policy=simple:undefine-attribute(name=name-preferences)")
                    ;
        }
    }
}
