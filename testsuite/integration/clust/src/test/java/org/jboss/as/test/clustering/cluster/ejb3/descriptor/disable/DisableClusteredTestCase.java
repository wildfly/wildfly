/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.descriptor.disable;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;

import java.util.Properties;

import javax.naming.NamingException;

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
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the @Clustered beans with <clustered>false</clustered> in jboss-ejb3.xml do not exhibit clustering behavior. This is
 * part of migration of tests from testsuite from prior JBoss versions to AS7 (JBQA-5855) - issue EJBTHREE-1346.
 *
 * @author Ondrej Chaloupka
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DisableClusteredTestCase {
    private static final Logger log = Logger.getLogger(DisableClusteredTestCase.class);
    private static final String ARCHIVE_NAME = "not-creating-cluster-dd";
    private static boolean node1Running = false;
    private static boolean node2Running = false;

    private static EJBDirectory directory;
    private static ContextSelector<EJBClientContext> previousSelector;
    
    private static final String PROPERTIES_FILENAME = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
    
    @ArquillianResource
    private ContainerController container;
    @ArquillianResource
    private Deployer deployer;

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
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(DisableClusteredTestCase.class.getPackage());
        jar.addClasses(NodeNameGetter.class);
        jar.addAsManifestResource(DisableClusteredTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        directory = new RemoteEJBDirectory(ARCHIVE_NAME);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        directory.close();
    }

    @Test
    @InSequence(-1)
    public void startContainers() {
        container.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        node1Running = true;
        container.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
        node2Running = true;
    }
    
    /**
     * Validate the stateful bean is not clustered by having failover not work
     */
    @Test
    @InSequence(1)
    public void testStatefulBean() throws Exception {
        previousSelector = EJBClientContextSelector
                .setup(PROPERTIES_FILENAME);
        DisableClusteredRemote stateful = directory.lookupStateful("DisableClusteredAnnotationStateful", DisableClusteredRemote.class);

        String node1 = stateful.getNodeState();
        log.info("Called node name: " + node1);

        // Now we switch off node 1, failover should not be provided
        if (node1.equals(NODE_1)) {
            container.stop(CONTAINER_1);
            node1Running = false;
        } else {
            container.stop(CONTAINER_2);
            node2Running = false;
        }

        try {
            stateful.getNodeState();
            Assert.fail("No failover should be provided but it was.");
        } catch (Exception good) {
            // it's supposed
        }
    }

    /**
     * Test stateless bean by demonstrating no load balancing
     */
    @Test
    @InSequence(2)
    public void testStatelessBean(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        
        String hostName = node1Running ? client1.getRemoteEjbURL().getHost() : client2.getRemoteEjbURL().getHost();
        int port = node1Running ? client1.getRemoteEjbURL().getPort() : client2.getRemoteEjbURL().getPort();
        Properties property = new Properties();
        property.setProperty("remote.connection.default.host", hostName);
        property.setProperty("remote.connection.default.port", Integer.toString(port));
        EJBClientContextSelector.setup(PROPERTIES_FILENAME, property);
        
        DisableClusteredRemote stateless = directory.lookupStateless("DisableClusteredAnnotationStateless", DisableClusteredRemote.class);

        if (!node1Running) {
            container.start(CONTAINER_1);
            node1Running = true;
        }
        if (!node2Running) {
            container.start(CONTAINER_2);
            node2Running = true;
        }

        String node1 = stateless.getNodeState();
        Assert.assertNotNull(node1);
        log.info("Called node name: " + node1);

        // testing that the load balancing won't be done
        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(node1, stateless.getNodeState());
        }
    }
    
    @Test
    @InSequence(3)
    public void stopAndUndeploy() {
        // returning to the previous context selector, @see {RemoteEJBClientDDBasedSFSBFailoverTestCase}
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
        
        if (!node1Running) {
            deployer.undeploy(DEPLOYMENT_1);
            container.stop(CONTAINER_1);
        }
        if (!node2Running) {
            deployer.undeploy(DEPLOYMENT_2);
            container.stop(CONTAINER_2);
        }
    }
}