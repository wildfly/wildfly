/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.infinispan.commons.util.Version;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.shared.TimeoutUtil;
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
 * convenience methods for managing container and deployment lifecycle ({@link #start(Set)}, {@link #deploy(Set)}, etc.).
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
    public static final Set<String> NODE_1_2 = Set.of(NODE_1, NODE_2);
    public static final Set<String> NODE_1_2_3 = Set.of(NODE_1, NODE_2, NODE_3);
    public static final Set<String> NODE_1_2_3_4 = Set.of(NODE_1, NODE_2, NODE_3, NODE_4);
    public static final String NODE_NAME_PROPERTY = "jboss.node.name";

    // Test deployment names
    public static final String DEPLOYMENT_1 = "deployment-1";
    public static final String DEPLOYMENT_2 = "deployment-2";
    public static final String DEPLOYMENT_3 = "deployment-3";
    public static final String DEPLOYMENT_4 = "deployment-4";
    public static final Set<String> DEPLOYMENT_1_2 = Set.of(DEPLOYMENT_1, DEPLOYMENT_2);
    public static final Set<String> DEPLOYMENT_1_2_3 = Set.of(DEPLOYMENT_1, DEPLOYMENT_2, DEPLOYMENT_3);
    public static final Set<String> DEPLOYMENT_1_2_3_4 = Set.of(DEPLOYMENT_1, DEPLOYMENT_2, DEPLOYMENT_3, DEPLOYMENT_4);

    // Helper deployment names
    public static final String DEPLOYMENT_HELPER_1 = "deployment-helper-0";
    public static final String DEPLOYMENT_HELPER_2 = "deployment-helper-1";
    public static final String DEPLOYMENT_HELPER_3 = "deployment-helper-2";
    public static final String DEPLOYMENT_HELPER_4 = "deployment-helper-3";
    public static final Set<String> DEPLOYMENT_HELPER_1_2 = Set.of(DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2);
    public static final Set<String> DEPLOYMENT_HELPER_1_2_3 = Set.of(DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2, DEPLOYMENT_HELPER_3);
    public static final Set<String> DEPLOYMENT_HELPER_1_2_3_4 = Set.of(DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2, DEPLOYMENT_HELPER_3, DEPLOYMENT_HELPER_4);

    // Infinispan Server
    public static final String INFINISPAN_SERVER_HOME = System.getProperty("infinispan.server.home");
    public static final String INFINISPAN_SERVER_PROFILE = Optional.ofNullable(System.getProperty("infinispan.server.profile")).filter(Predicate.not(String::isBlank)).orElse(String.format("infinispan-%s.xml", Version.getMajorMinor()));
    public static final String INFINISPAN_SERVER_ADDRESS = "127.0.0.1";
    public static final int INFINISPAN_SERVER_PORT = 11222;
    public static final String INFINISPAN_APPLICATION_USER = "testsuite-application-user";
    public static final String INFINISPAN_APPLICATION_PASSWORD = "testsuite-application-password";

    // Undertow-based WildFly load-balancer
    public static final String LOAD_BALANCER_1 = "load-balancer-1";

    // H2 database
    public static final String DB_PORT = System.getProperty("dbport", "9092");

    // Timeouts
    public static final int GRACE_TIME_TO_REPLICATE = TimeoutUtil.adjust(4000);
    public static final int GRACE_TIME_TOPOLOGY_CHANGE = TimeoutUtil.adjust(3000);
    public static final int SUSPEND_TIMEOUT_S = TimeoutUtil.adjust(60);
    public static final int GRACE_TIME_TO_MEMBERSHIP_CHANGE = TimeoutUtil.adjust(10000);
    public static final int WAIT_FOR_PASSIVATION_MS = TimeoutUtil.adjust(5);
    public static final int HTTP_REQUEST_WAIT_TIME_S = TimeoutUtil.adjust(5);

    // System Properties
    public static final String TESTSUITE_NODE0 = System.getProperty("node0", "127.0.0.1");
    public static final String TESTSUITE_NODE1 = System.getProperty("node1", "127.0.0.1");
    public static final String TESTSUITE_NODE2 = System.getProperty("node2", "127.0.0.1");
    public static final String TESTSUITE_NODE3 = System.getProperty("node3", "127.0.0.1");

    protected static final Logger log = Logger.getLogger(AbstractClusteringTestCase.class);
    private static final Map<String, String> CONTAINER_TO_DEPLOYMENT = Map.of(NODE_1, DEPLOYMENT_1, NODE_2, DEPLOYMENT_2, NODE_3, DEPLOYMENT_3, NODE_4, DEPLOYMENT_4);

    protected static Map.Entry<String, String> parseSessionRoute(HttpResponse response) {
        Header setCookieHeader = response.getFirstHeader("Set-Cookie");
        if (setCookieHeader == null) return null;
        String setCookieValue = setCookieHeader.getValue();
        final String id = setCookieValue.substring(setCookieValue.indexOf('=') + 1, setCookieValue.indexOf(';'));
        final int index = id.indexOf('.');
        return (index < 0) ? new AbstractMap.SimpleImmutableEntry<>(id, null) : new AbstractMap.SimpleImmutableEntry<>(id.substring(0, index), id.substring(index + 1));
    }

    @ArquillianResource
    protected ContainerRegistry containerRegistry;
    @ArquillianResource
    protected WildFlyContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    private final Set<String> containers;
    private final Set<String> deployments;

    // Framework contract methods
    public AbstractClusteringTestCase() {
        this(NODE_1_2);
    }

    public AbstractClusteringTestCase(Set<String> containers) {
        this(containers, toDeployments(containers));
    }

    public AbstractClusteringTestCase(Set<String> containers, Set<String> deployments) {
        this.containers = Set.copyOf(containers);
        this.deployments = Set.copyOf(deployments);
    }

    /**
     * Returns an unmodifiable set of container names available for this clustering test.
     *
     * @return an unmodifiable set of container names available for this clustering test.
     */
    public Set<String> getContainers() {
        return this.containers;
    }

    /**
     * Returns an unmodifiable set of deployment names available for this clustering test.
     *
     * @return an unmodifiable set of deployment names available for this clustering test.
     */
    public Set<String> getDeployments() {
        return this.deployments;
    }

    /**
     * Guarantees that prior to test method execution
     * (1) all containers that are not used in the test are stopped and,
     * (2) all requested containers are running and,
     * (3) all requested deployments are deployed thus allowing all necessary test resource injection.
     */
    @Before
    public void beforeTestMethod() throws Exception {
        this.containerRegistry.getContainers().forEach(container -> {
            if (container.getState() == Container.State.STARTED && !this.containers.contains(container.getName())) {
                // Even though we should be able to just stop the container object this currently fails with:
                // WFARQ-47 Calling "container.stop();" always ends exceptionally "Caught exception closing ManagementClient: java.lang.NullPointerException"
                this.stop(container.getName());
                log.debugf("Stopped container '%s' which was started but not requested for this test.", container.getName());
            }
        });

        this.start();
        this.deploy();
    }

    /**
     * Guarantees that all deployments are undeployed after the test method has been executed.
     */
    @After
    public void afterTestMethod() throws Exception {
        this.start();
        this.undeploy();
    }

    // Node and deployment lifecycle management convenience methods

    // Container.start(..) methods

    protected boolean isStarted(String container) {
        return NodeUtil.isStarted(this.controller, container);
    }

    protected void start() {
        this.start(this.containers);
    }

    protected void start(String container) {
        NodeUtil.start(this.controller, container);
    }

    protected void start(Set<String> containers) {
        NodeUtil.start(this.controller, containers);
    }

    // Container.stop(..) methods

    // n.b. all stop methods for the purposes of clustering tests by default use a suspend-timeout;
    // use 0 explicitly to not wait for active sessions to finish

    /**
     * Gracefully stops all containers configured for this test case with default suspend timeout of 60 seconds (adjusted).
     * To stop the containers without waiting for all requests to finish, use {@link #stop(java.lang.String, int)} with suspend timeout of 0.
     */
    protected void stop() {
        this.stop(this.containers, SUSPEND_TIMEOUT_S);
    }

    /**
     * Gracefully stops given container with default suspend timeout of 60 seconds (adjusted).
     * To stop the container without waiting for all requests to finish, use {@link #stop(java.lang.String, int)} with suspend timeout of 0.
     */
    protected void stop(String container) {
        NodeUtil.stop(this.controller, container, SUSPEND_TIMEOUT_S);
    }

    /**
     * Sequentially gracefully stops given set of containers with default suspend timeout of 60 seconds (adjusted).
     * To stop the containers without waiting for all requests to finish, use {@link #stop(java.lang.String, int)} with suspend timeout of 0.
     */
    protected void stop(Set<String> containers) {
        this.stop(containers, SUSPEND_TIMEOUT_S);
    }

    /**
     * Gracefully stops given container with a given suspend timeout.
     */
    protected void stop(String container, int suspendTimeout) {
        NodeUtil.stop(this.controller, container, suspendTimeout);
    }

    /**
     * Gracefully stops given set of containers with a given suspend timeout.
     */
    protected void stop(Set<String> containers, int suspendTimeout) {
        NodeUtil.stop(this.controller, containers, suspendTimeout);
    }

    // Container deployment methods

    protected void deploy() {
        this.deploy(this.deployments);
    }

    protected void deploy(String deployment) {
        NodeUtil.deploy(this.deployer, deployment);
    }

    protected void deploy(Set<String> deployments) {
        NodeUtil.deploy(this.deployer, deployments);
    }

    protected void undeploy() {
        this.undeploy(this.deployments);
    }

    protected void undeploy(String deployment) {
        NodeUtil.undeploy(this.deployer, deployment);
    }

    protected void undeploy(Set<String> deployments) {
        NodeUtil.undeploy(this.deployer, deployments);
    }

    protected static String findDeployment(String container) {
        String deployment = CONTAINER_TO_DEPLOYMENT.get(container);
        if (deployment == null) {
            throw new IllegalArgumentException(container);
        }
        return deployment;
    }

    public static int getPortOffsetForNode(String node) {
        return switch (node) {
            case NODE_1 -> 0;
            case NODE_2 -> 100;
            case NODE_3 -> 200;
            case NODE_4 -> 300;
            default -> throw new IllegalArgumentException();
        };
    }

    static Set<String> toDeployments(Set<String> containers) {
        return containers.stream().map(AbstractClusteringTestCase::findDeployment).collect(Collectors.toSet());
    }

    public interface Lifecycle {
        default void start(String container) {
            this.start(Set.of(container));
        }
        default void stop(String container) {
            this.stop(Set.of(container));
        }
        void start(Set<String> containers);
        void stop(Set<String> containers);
    }

    /**
     * A simple restart lifecycle which does *not* perform a graceful shutdown.
     * In most cases the {@link GracefulRestartLifecycle} should be used instead.
     *
     * @see GracefulRestartLifecycle
     */
    public class RestartLifecycle implements Lifecycle {
        @Override
        public void start(Set<String> containers) {
            AbstractClusteringTestCase.this.start(containers);
        }

        @Override
        public void stop(Set<String> containers) {
            AbstractClusteringTestCase.this.stop(containers, 0);
        }
    }

    public class GracefulRestartLifecycle extends RestartLifecycle {
        @Override
        public void stop(Set<String> containers) {
            AbstractClusteringTestCase.this.stop(containers);
        }
    }

    public class RedeployLifecycle implements Lifecycle {
        @Override
        public void start(Set<String> containers) {
            AbstractClusteringTestCase.this.deploy(toDeployments(containers));
        }

        @Override
        public void stop(Set<String> containers) {
            AbstractClusteringTestCase.this.undeploy(toDeployments(containers));
        }
    }
}
