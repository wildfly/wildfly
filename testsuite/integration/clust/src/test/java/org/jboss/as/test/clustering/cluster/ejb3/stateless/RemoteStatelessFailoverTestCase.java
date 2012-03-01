/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.stateless;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINERS;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENTS;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.GRACE_TIME_TO_MEMBERSHIP_CHANGE;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODES;
import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.Stateless;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.StatelessBean;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteStatelessFailoverTestCase {

    private static final String MODULE_NAME = "remote-ejb-client-stateless-bean-failover-test";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";
    private static EJBDirectory context;

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private boolean[] deployed = new boolean[] { false, false };
    private boolean[] started = new boolean[] { false, false };

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(StatelessBean.class.getPackage());
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    @Test
    public void testFailoverOnStop() throws Exception {
        this.start(0);
        this.deploy(0);

        final ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        
        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);
            
            assertEquals(NODES[0], bean.getNodeName());

            this.start(1);
            this.deploy(1);

            // Allow ample time for topology change to propagate to client
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);
            
            this.stop(0);
            
            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
            // shutdown the containers
            for (int i = 0; i < NODES.length; ++i) {
                this.undeploy(i);
                this.stop(i);
            }
        }
    }

    @Test
    public void testFailoverOnUndeploy() throws Exception {
        // Container is unmanaged, so start it ourselves
        this.start(0);
        this.deploy(0);

        final ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);
        
        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);
            
            assertEquals(NODES[0], bean.getNodeName());

            this.start(1);
            this.deploy(1);
            
            // Allow ample time for topology change to propagate to client
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);
            
            this.undeploy(0);
            
            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
            // shutdown the containers
            for (int i = 0; i < NODES.length; ++i) {
                this.undeploy(i);
                this.stop(i);
            }
        }
    }

    private void deploy(int index) {
        if (this.started[index] && !this.deployed[index]) {
            this.deployer.deploy(DEPLOYMENTS[index]);
            this.deployed[index] = true;
        }
    }

    private void undeploy(int index) {
        if (this.started[index] && this.deployed[index]) {
            this.deployer.undeploy(DEPLOYMENTS[index]);
            this.deployed[index] = false;
        }
    }
    
    private void start(int index) {
        if (!this.started[index]) {
            this.container.start(CONTAINERS[index]);
            this.started[index] = true;
        }
    }
    
    private void stop(int index) {
        if (this.started[index]) {
            this.container.stop(CONTAINERS[index]);
            this.started[index] = false;
        }
    }
}
