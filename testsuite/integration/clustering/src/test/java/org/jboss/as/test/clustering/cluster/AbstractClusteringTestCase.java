/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * Base implementation for every clustering test which guarantees a framework contract as follows:
 * <ol>
 * <li>test case is constructed specifying nodes and deployments required</li>
 * <li>before every test method, first all containers that are not required in the test are stopped</li>
 * <li>before every test method, containers are started and deployments are deployed via {@link #beforeTestMethod()}</li>
 * <li>after every method execution the deployments are undeployed via {@link #afterTestMethod()}</li>
 * </ol>
 * Should the test demand different node and deployment handling, the {@link #beforeTestMethod()} and {@link #afterTestMethod()}
 * may be overridden.
 * <p>
 * Furthermore, this base class provides common constants for node/instance-id, deployment/deployment helpers, timeouts and provides
 * convenience methods for managing container and deployment lifecycle ({@link #start(String...)}, {@link #deploy(String...)}, etc).
 *
 * @author Radoslav Husar
 */
public abstract class AbstractClusteringTestCase {

    public static final String CONTAINER_SINGLE = "node-non-ha";

    // Unified node and container names
    public static final String NODE_1 = "node-1";
    public static final String NODE_2 = "node-2";
    public static final String NODE_3 = "node-3";
    public static final String NODE_4 = "node-4";
    public static final String[] TWO_NODES = new String[] { NODE_1, NODE_2 };
    public static final String[] THREE_NODES = new String[] { NODE_1, NODE_2, NODE_3 };
    public static final String[] FOUR_NODES = new String[] { NODE_1, NODE_2, NODE_3, NODE_4 };
    public static final String NODE_NAME_PROPERTY = "jboss.node.name";

    // Test deployment names
    public static final String DEPLOYMENT_1 = "deployment-1";
    public static final String DEPLOYMENT_2 = "deployment-2";
    public static final String DEPLOYMENT_3 = "deployment-3";
    public static final String DEPLOYMENT_4 = "deployment-4";
    public static final String[] TWO_DEPLOYMENTS = new String[] { DEPLOYMENT_1, DEPLOYMENT_2 };
    public static final String[] FOUR_DEPLOYMENTS = new String[] { DEPLOYMENT_1, DEPLOYMENT_2, DEPLOYMENT_3, DEPLOYMENT_4 };

    // Helper deployment names
    public static final String DEPLOYMENT_HELPER_1 = "deployment-helper-0";
    public static final String DEPLOYMENT_HELPER_2 = "deployment-helper-1";
    public static final String DEPLOYMENT_HELPER_3 = "deployment-helper-2";
    public static final String DEPLOYMENT_HELPER_4 = "deployment-helper-3";
    public static final String[] TWO_DEPLOYMENT_HELPERS = new String[] { DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2 };
    public static final String[] FOUR_DEPLOYMENT_HELPERS = new String[] { DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2, DEPLOYMENT_HELPER_3, DEPLOYMENT_HELPER_4 };

    // Infinispan Server
    public static final String INFINISPAN_SERVER_1 = "infinispan-server-1";

    // Timeouts
    public static final int GRACE_TIME_TO_REPLICATE = TimeoutUtil.adjust(3000);
    public static final int GRACE_TIME_TOPOLOGY_CHANGE = TimeoutUtil.adjust(3000);
    public static final int GRACEFUL_SHUTDOWN_TIMEOUT = TimeoutUtil.adjust(15);
    public static final int GRACE_TIME_TO_MEMBERSHIP_CHANGE = TimeoutUtil.adjust(10000);
    public static final int WAIT_FOR_PASSIVATION_MS = TimeoutUtil.adjust(5);
    public static final int HTTP_REQUEST_WAIT_TIME_S = TimeoutUtil.adjust(5);

    // System Properties
    public static final String TESTSUITE_NODE0 = System.getProperty("node0", "127.0.0.1");
    public static final String TESTSUITE_NODE1 = System.getProperty("node1", "127.0.0.1");
    public static final String TESTSUITE_NODE2 = System.getProperty("node2", "127.0.0.1");
    public static final String TESTSUITE_NODE3 = System.getProperty("node3", "127.0.0.1");
    public static final String TESTSUITE_MCAST = System.getProperty("mcast", "230.0.0.4");
    public static final String TESTSUITE_MCAST1 = System.getProperty("mcast1", "230.0.0.5");
    public static final String TESTSUITE_MCAST2 = System.getProperty("mcast2", "230.0.0.6");
    public static final String TESTSUITE_MCAST3 = System.getProperty("mcast3", "230.0.0.7");

    protected static final Logger log = Logger.getLogger(AbstractClusteringTestCase.class);
    private static final RoutingSupport routing = new SimpleRoutingSupport();
    private static final Map<String, String> NODE_TO_DEPLOYMENT = new TreeMap<>();
    static {
        NODE_TO_DEPLOYMENT.put(NODE_1, DEPLOYMENT_1);
        NODE_TO_DEPLOYMENT.put(NODE_2, DEPLOYMENT_2);
        NODE_TO_DEPLOYMENT.put(NODE_3, DEPLOYMENT_3);
        NODE_TO_DEPLOYMENT.put(NODE_4, DEPLOYMENT_4);
    }

    protected static Map.Entry<String, String> parseSessionRoute(HttpResponse response) {
        Header setCookieHeader = response.getFirstHeader("Set-Cookie");
        if (setCookieHeader == null) return null;
        String setCookieValue = setCookieHeader.getValue();
        return routing.parse(setCookieValue.substring(setCookieValue.indexOf('=') + 1, setCookieValue.indexOf(';')));
    }

    @ArquillianResource
    protected ContainerRegistry containerRegistry;
    @ArquillianResource
    protected WildFlyContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    protected final String[] nodes;
    protected final String[] deployments;

    // Framework contract methods

    public AbstractClusteringTestCase() {
        this(TWO_NODES);
    }

    public AbstractClusteringTestCase(String[] nodes) {
        this(nodes, Stream.of(nodes).map(NODE_TO_DEPLOYMENT::get).toArray(String[]::new));
    }

    public AbstractClusteringTestCase(String[] nodes, String[] deployments) {
        this.nodes = nodes;
        this.deployments = deployments;
    }

    /**
     * Guarantees that prior to test method execution
     * (1) all containers that are not used in the test are stopped and,
     * (2) all requested containers are running and,
     * (3) all requested deployments are deployed thus allowing all necessary test resources injection.
     */
    @Before
    public void beforeTestMethod() throws Exception {
        this.containerRegistry.getContainers().forEach(container -> {
            if (container.getState() == Container.State.STARTED && !Arrays.asList(nodes).contains(container.getName())) {
                // Even though we should be able to just stop the container object this currently fails with:
                // WFARQ-47 Calling "container.stop();" always ends exceptionally "Caught exception closing ManagementClient: java.lang.NullPointerException"
                this.stop(container.getName());
                log.infof("Stopped container '%s' which was started but not requested for this test.", container.getName());
            }
        });

        NodeUtil.start(this.controller, nodes);
        NodeUtil.deploy(this.deployer, deployments);
    }

    /**
     * Guarantees that all deployments are undeployed after the test method has been executed.
     */
    @After
    public void afterTestMethod() throws Exception {
        NodeUtil.start(this.controller, nodes);
        NodeUtil.undeploy(this.deployer, deployments);
    }

    // Node and deployment lifecycle management convenience methods

    protected void start(String... containers) {
        NodeUtil.start(this.controller, containers);
    }

    protected void stop(String... containers) {
        NodeUtil.stop(this.controller, containers);
    }

    protected void stop(int timeout, String... containers) {
        NodeUtil.stop(this.controller, timeout, containers);
    }

    protected void deploy(String... deployments) {
        NodeUtil.deploy(this.deployer, deployments);
    }

    protected void undeploy(String... deployments) {
        NodeUtil.undeploy(this.deployer, deployments);
    }

    protected String findDeployment(String node) {
        return NODE_TO_DEPLOYMENT.get(node);
    }

    public interface Lifecycle {
        void start(String... nodes);
        void stop(String... nodes);
    }

    public class RestartLifecycle implements Lifecycle {
        @Override
        public void start(String... nodes) {
            AbstractClusteringTestCase.this.start(this.getContainers(nodes));
        }

        @Override
        public void stop(String... nodes) {
            AbstractClusteringTestCase.this.stop(this.getContainers(nodes));
        }

        String[] getContainers(String... nodes) {
            String[] containers = new String[nodes.length];
            for (int i = 0; i < nodes.length; ++i) {
                String node = nodes[i];
                if (node == null) {
                    throw new IllegalArgumentException();
                }
                containers[i] = node;
            }
            return containers;
        }
    }

    public class GracefulRestartLifecycle extends RestartLifecycle {
        @Override
        public void stop(String... nodes) {
            AbstractClusteringTestCase.this.stop(GRACEFUL_SHUTDOWN_TIMEOUT, this.getContainers(nodes));
        }
    }

    public class RedeployLifecycle implements Lifecycle {
        @Override
        public void start(String... nodes) {
            AbstractClusteringTestCase.this.deploy(this.getDeployments(nodes));
        }

        @Override
        public void stop(String... nodes) {
            AbstractClusteringTestCase.this.undeploy(this.getDeployments(nodes));
        }

        private String[] getDeployments(String... nodes) {
            String[] deployments = new String[nodes.length];
            for (int i = 0; i < nodes.length; ++i) {
                String node = nodes[i];
                String deployment = NODE_TO_DEPLOYMENT.get(node);
                if (deployment == null) {
                    throw new IllegalArgumentException(node);
                }
                deployments[i] = deployment;
            }
            return deployments;
        }
    }
}
