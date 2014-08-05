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

import java.util.Map;
import java.util.TreeMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusteringTestConstants;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Base cluster test that guarantees a framework contract as follows:
 * <ul>
 * <li>before every test method, containers are started and deployments are deployed via {@link #beforeTestMethod()}</li>
 * <li>after every method execution the deployments are undeployed via {@link #afterTestMethod()}</li>
 * </ul>
 *
 * Should the test demand different node/deployment setup, the methods must be overridden.
 *
 * Furthermore provides convenience methods for {@link NodeUtil} utility ({@link #start(String)},
 * {@link #deploy(String)}, etc).
 *
 * @author Radoslav Husar
 * @version Jul 2013
 */
public abstract class ClusterAbstractTestCase implements ClusteringTestConstants {

    protected static final Logger log = Logger.getLogger(ClusterAbstractTestCase.class);
    private static final RoutingSupport routing = new SimpleRoutingSupport();
    static final Map<String, String> NODE_TO_CONTAINER = new TreeMap<>();
    static final Map<String, String> NODE_TO_DEPLOYMENT = new TreeMap<>();
    static {
        NODE_TO_CONTAINER.put(NODE_1, CONTAINER_1);
        NODE_TO_CONTAINER.put(NODE_2, CONTAINER_2);
        NODE_TO_DEPLOYMENT.put(NODE_1, DEPLOYMENT_1);
        NODE_TO_DEPLOYMENT.put(NODE_2, DEPLOYMENT_2);
    }

    protected static Map.Entry<String, String> parseSessionRoute(HttpResponse response) {
        Header setCookieHeader = response.getFirstHeader("Set-Cookie");
        if (setCookieHeader == null) return null;
        String setCookieValue = setCookieHeader.getValue();
        return routing.parse(setCookieValue.substring(setCookieValue.indexOf('=') + 1, setCookieValue.indexOf(';')));
    }

    @ArquillianResource
    protected ContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    // Framework contract methods

    /**
     * Guarantees that prior to test method execution both containers are running and both deployments are deployed.
     */
    @Before
    @RunAsClient // Does not work, see https://issues.jboss.org/browse/ARQ-351
    public void beforeTestMethod() {
        NodeUtil.start(this.controller, CONTAINERS);
        NodeUtil.deploy(this.deployer, DEPLOYMENTS);
    }

    /**
     * Guarantees that all deployments are undeployed after the test method has been executed.
     */
    @After
    @RunAsClient // Does not work, see https://issues.jboss.org/browse/ARQ-351
    public void afterTestMethod() {
        NodeUtil.start(this.controller, CONTAINERS);
        NodeUtil.undeploy(this.deployer, DEPLOYMENTS);
    }

    // Node and deployment lifecycle management convenience methods

    protected void start(String... containers) {
        NodeUtil.start(this.controller, containers);
    }

    protected void stop(String... containers) {
        NodeUtil.stop(this.controller, containers);
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

    protected String findContainer(String node) {
        return NODE_TO_CONTAINER.get(node);
    }

    /**
     * Printout some debug info.
     */
    @BeforeClass
    public static void printSystemProperties() {
        // Enable for debugging if you like:
        //Properties systemProperties = System.getProperties();
        //log.info("System properties:\n" + systemProperties);
    }

    public interface Lifecycle {
        void start(String... nodes);
        void stop(String... nodes);
    }

    public class RestartLifecycle implements Lifecycle {
        @Override
        public void start(String... nodes) {
            ClusterAbstractTestCase.this.start(this.getContainers(nodes));
        }

        @Override
        public void stop(String... nodes) {
            ClusterAbstractTestCase.this.stop(this.getContainers(nodes));
        }

        private String[] getContainers(String... nodes) {
            String[] containers = new String[nodes.length];
            for (int i = 0; i < nodes.length; ++i) {
                String node = nodes[i];
                String container = NODE_TO_CONTAINER.get(node);
                if (container == null) {
                    throw new IllegalArgumentException(node);
                }
                containers[i] = container;
            }
            return containers;
        }
    }

    public class RedeployLifecycle implements Lifecycle {
        @Override
        public void start(String... nodes) {
            ClusterAbstractTestCase.this.deploy(this.getDeployments(nodes));
        }

        @Override
        public void stop(String... nodes) {
            ClusterAbstractTestCase.this.undeploy(this.getDeployments(nodes));
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
