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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusteringTestConstants;
import org.jboss.as.test.clustering.NodeUtil;
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
        this.start(CONTAINERS);
        this.deploy(DEPLOYMENTS);
    }

    /**
     * Guarantees that all deployments are undeployed after the test method has been executed.
     */
    @After
    @RunAsClient // Does not work, see https://issues.jboss.org/browse/ARQ-351
    public void afterTestMethod() {
        this.start(CONTAINERS);
        this.undeploy(DEPLOYMENTS);
    }

    // Node and deployment lifecycle management convenience methods

    protected void start(String container) {
        NodeUtil.start(controller, container);
    }

    protected void start(String[] containers) {
        NodeUtil.start(controller, containers);
    }

    protected void stop(String container) {
        NodeUtil.stop(controller, container);
    }

    protected void stop(String[] containers) {
        NodeUtil.stop(controller, containers);
    }

    protected void deploy(String deployment) {
        NodeUtil.deploy(deployer, deployment);
    }

    protected void deploy(String[] deployments) {
        NodeUtil.deploy(deployer, deployments);
    }

    protected void undeploy(String deployments) {
        NodeUtil.undeploy(deployer, deployments);
    }

    protected void undeploy(String[] deployments) {
        NodeUtil.undeploy(deployer, deployments);
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

}
