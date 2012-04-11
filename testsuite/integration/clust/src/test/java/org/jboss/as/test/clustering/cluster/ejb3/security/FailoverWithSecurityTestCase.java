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

package org.jboss.as.test.clustering.cluster.ejb3.security;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.NodeInfoServlet;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test security login on failover.
 * 
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FailoverWithSecurityTestCase {
    private static Logger log = Logger.getLogger(FailoverWithSecurityTestCase.class);

    private static final String PROPERTIES_FILE = "cluster/ejb3/security/jboss-ejb-client.properties";
    private static final String ARCHIVE_NAME = "cluster-security-domain-test";
    private static RemoteEJBDirectory context;

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    private static Archive<?> createDeployment() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        war.addPackage(FailoverWithSecurityTestCase.class.getPackage());
        war.addClasses(NodeNameGetter.class, NodeInfoServlet.class);
        log.info(war.toString(true));
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(ARCHIVE_NAME);
    }

    @Test
    @InSequence(-1)
    public void startServers() {
        // Container is unmanaged, need to start manually.
        // This is a little hacky @see https://community.jboss.org/thread/176096
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    @Test
    @InSequence(1)
    public void testDomainSecurityAnnotation(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        
        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup(PROPERTIES_FILE);
        
        try {
            BeanRemote statelessBean = context.lookupStateless(StatelessBean.class, BeanRemote.class);
            BeanRemote statefulBean = context.lookupStateful(StatefulBean.class, BeanRemote.class);

            String statelessNodeName = statelessBean.getNodeName();
            String statefulNodeName = statefulBean.getNodeName();
            Assert.assertNotNull("No name was returned from SLSB ", statelessNodeName);
            Assert.assertNotNull("No name was returned from SFSB ", statefulNodeName);
            
            String stoppedContainer = null;
            String runningNode = null;
            if(NODE_1.equals(statefulNodeName)) {
                stoppedContainer = CONTAINER_1;
                runningNode = NODE_2;
            } else {
                stoppedContainer = CONTAINER_2;
                runningNode = NODE_1;
            }
            controller.stop(stoppedContainer);
            
            statelessNodeName = statelessBean.getNodeName();
            statefulNodeName = statefulBean.getNodeName();
            Assert.assertEquals("SLSB has to return the only running node " + runningNode, runningNode, statelessNodeName);
            Assert.assertEquals("SFSB has to return the only running node " + runningNode, runningNode, statefulNodeName);
            
            if(CONTAINER_1.equals(stoppedContainer)) {
                controller.start(CONTAINER_1);
                deployer.undeploy(DEPLOYMENT_2);
                controller.stop(CONTAINER_2);
                runningNode = NODE_1;
            } else {
                controller.start(CONTAINER_2);
                deployer.undeploy(DEPLOYMENT_1);
                controller.stop(CONTAINER_1);
                runningNode = NODE_2;
            }
            
            statelessNodeName = statelessBean.getNodeName();
            statefulNodeName = statefulBean.getNodeName();
            Assert.assertEquals("SLSB has to return the only running node " + runningNode, runningNode, statelessNodeName);
            Assert.assertEquals("SFSB has to return the only running node " + runningNode, runningNode, statefulNodeName);
            
        } finally {
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }
    }

    @Test
    @InSequence(100)
    public void stopServers(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        if (client1.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
        }
        if (client2.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
    }
}
