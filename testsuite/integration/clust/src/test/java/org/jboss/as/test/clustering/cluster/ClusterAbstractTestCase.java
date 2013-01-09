/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINERS;

import java.util.Properties;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Base cluster test that ensures the custom containers are already running.
 *
 * @author Radoslav Husar
 * @version Oct 2012
 */
public abstract class ClusterAbstractTestCase {

    protected static final Logger log = Logger.getLogger(ClusterAbstractTestCase.class);

    @ArquillianResource
    protected ContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    /**
     * Ensure the containers are running, otherwise start them.
     */
    @Test
    @InSequence(-1)
    @RunAsClient
    public void testSetup() {
        this.setUp();
    }

    /**
     * Override this method for different behavior of the test.
     */
    protected void setUp() {
        NodeUtil.start(controller, CONTAINERS);
    }

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
        Properties systemProperties = System.getProperties();
        log.info("System properties:\n" + systemProperties);
    }

}
