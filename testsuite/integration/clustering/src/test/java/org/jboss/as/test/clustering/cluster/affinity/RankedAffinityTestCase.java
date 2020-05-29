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

package org.jboss.as.test.clustering.cluster.affinity;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.PropertyPermission;

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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for ranked affinity support in the server and in the Undertow-based mod_cluster load balancer.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(RankedAffinityTestCase.ServerSetupTask.class)
public class RankedAffinityTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = RankedAffinityTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";
    private static final String BALANCER_NAME = "mycluster";

    public RankedAffinityTestCase() {
        super(new String[] { NODE_1, NODE_2, NODE_3, LOAD_BALANCER_1 }, THREE_DEPLOYMENTS);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return deployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return deployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return deployment();
    }

    private static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(SimpleServlet.class, Mutable.class)
                .setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml");
        return war;
    }

    /**
     * Test ranked routing by (1) creating a session on a node, (2) disabling the node that served the request using the load
     * balancer API and (3) verifying correct affinity on subsequent request.
     */
    @Test
    public void test(@ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL) throws Exception {
        URL lbURL = new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort() + 500, baseURL.getFile());
        URI lbURI = SimpleServlet.createURI(lbURL);

        establishTopology(baseURL, THREE_NODES);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            String[] previousAffinities;
            int value = 1;

            // 1
            HttpResponse response = client.execute(new HttpGet(lbURI));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                previousAffinities = entry.getValue().split("\\.");

                log.debugf("Response #1: %s", response);

                Assert.assertNotNull(entry);
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // TODO remove workaround for "UNDERTOW-1585 mod_cluster nodes are added in ERROR state" for which we need to wait until first status for ranked affinity to work
            Thread.sleep(3_000);

            // 2
            ModelControllerClient lbModelControllerClient = TestSuiteEnvironment.getModelControllerClient(null, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort() + 500);
            try (ManagementClient lbManagementClient = new ManagementClient(lbModelControllerClient, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort() + 500, "remote+http")) {
                String node = previousAffinities[0];
                ModelNode operation = new ModelNode();
                operation.get(OP).set(ModelDescriptionConstants.STOP);
                PathAddress address = PathAddress.parseCLIStyleAddress(String.format("/subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=%s/node=%s", BALANCER_NAME, node));
                operation.get(OP_ADDR).set(address.toModelNode());
                ModelNode result = lbManagementClient.getControllerClient().execute(operation);
                log.debugf("Running operation: %s", operation.toJSONString(false));
                Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
                log.debugf("Operation result: %s", result.toJSONString(false));

                operation = new ModelNode();
                operation.get(OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
                address = PathAddress.parseCLIStyleAddress("/subsystem=undertow/configuration=filter/mod-cluster=load-balancer");
                operation.get(OP_ADDR).set(address.toModelNode());
                operation.get(ModelDescriptionConstants.RECURSIVE).set(ModelNode.TRUE);
                operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(ModelNode.TRUE);
                result = lbManagementClient.getControllerClient().execute(operation);
                log.debugf("Running operation: %s", operation.toJSONString(false));
                Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
                log.debugf("Operation result: %s", result.toJSONString(false));

                Thread.sleep(GRACE_TIME_TOPOLOGY_CHANGE);

                // 3
                response = client.execute(new HttpGet(lbURI));
                try {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                    Assert.assertEquals("The subsequent request should have been served by the 2nd node in the affinity list", previousAffinities[1], response.getFirstHeader(SimpleServlet.HEADER_NODE_NAME).getValue());
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
            }
        }
    }

    private static void establishTopology(URL baseURL, String... nodes) throws URISyntaxException, IOException {
        ClusterHttpClientUtil.establishTopology(baseURL, "web", DEPLOYMENT_NAME, nodes);
    }

    static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder
                    .node(THREE_NODES)
                    .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=load-balancer-1:add(host=localhost,port=8590)")
                    .setup("/subsystem=modcluster/proxy=default:write-attribute(name=proxies,value=[load-balancer-1])")
                    .setup("/subsystem=modcluster/proxy=default:write-attribute(name=status-interval,value=1)")
                    .setup("/subsystem=distributable-web/infinispan-session-management=default/affinity=ranked:add")
                    .teardown("/subsystem=distributable-web/infinispan-session-management=default/affinity=primary-owner:add")
                    .teardown("/subsystem=modcluster/proxy=default:undefine-attribute(name=status-interval)")
                    .teardown("/subsystem=modcluster/proxy=default:undefine-attribute(name=proxies)")
                    .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=load-balancer-1:remove")
            ;
        }
    }
}
