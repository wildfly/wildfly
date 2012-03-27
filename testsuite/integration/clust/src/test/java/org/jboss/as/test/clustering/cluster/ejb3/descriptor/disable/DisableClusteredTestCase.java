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

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.NodeNameGetter;
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

    private static InitialContext context;
    private static ContextSelector<EJBClientContext> previousSelector; 
    
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
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(env);
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
                .setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");
        DisableClusteredRemote stateful = (DisableClusteredRemote) context.lookup("ejb:/" + ARCHIVE_NAME +
                "//DisableClusteredAnnotationStateful!" + DisableClusteredRemote.class.getName() + "?stateful");

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
    public void testStatelessBean() throws Exception {
        EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");
        
        DisableClusteredRemote stateless = (DisableClusteredRemote) context.lookup("ejb:/" + ARCHIVE_NAME +
                "//DisableClusteredAnnotationStateless!" + DisableClusteredRemote.class.getName());

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