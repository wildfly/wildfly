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

package org.jboss.as.test.manualmode.ejb.client.cluster;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Tests that clustered EJB remote invocations between the server and client, where the client itself
 * is another server instance, work correctly and the clustering failover capabilities are available. This
 * further tests that the cluster configurations in the jboss-ejb-client.xml are honoured.
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-4099
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientClusterConfigurationTestCase {

    private static final Logger logger = Logger.getLogger(EJBClientClusterConfigurationTestCase.class);

    private static final String MODULE_NAME = "server-to-server-clustered-ejb-invocation";

    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION = "jbossas-with-remote-outbound-connection";

    private static final String DEFAULT_AS_DEPLOYMENT = "default-jbossas-deployment";
    private static final String DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML = "other-deployment";

    // These should match what's configured in arquillian.xml for -Djboss.node.name of each server instance
    private static final String DEFAULT_JBOSSAS_NODE_NAME = "default-jbossas";
    private static final String JBOSSAS_WITH_OUTBOUND_CONNECTION_NODE_NAME = "jbossas-with-remote-outbound-connection";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private static Context context;
    private static ContextSelector<EJBClientContext> previousClientContextSelector;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
        // setup the client context selector
        previousClientContextSelector = setupEJBClientContextSelector();

    }

    @AfterClass
    public static void afterClass() {
        if (previousClientContextSelector != null) {
            EJBClientContext.setSelector(previousClientContextSelector);
        }
    }

    @Deployment(name = DEFAULT_AS_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive createContainer1Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClasses(ClusteredStatefulNodeNameEcho.class, NodeNameEcho.class);
        return ejbJar;
    }

    @Deployment(name = DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML, managed = false, testable = false)
    @TargetsContainer(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION)
    public static Archive createContainer2Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClasses(ClusteredStatefulNodeNameEcho.class, CustomDeploymentNodeSelector.class, NonClusteredStatefulNodeNameEcho.class, NodeNameEcho.class, ApplicationSpecificClusterNodeSelector.class);
        ejbJar.addAsManifestResource(NonClusteredStatefulNodeNameEcho.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbJar;
    }

    /**
     * 1) Start a server (A) which has a remote outbound connection to another server (B).
     * 2) Both server A and B have clustering capability and have a deployment containing a clustered SFSB.
     * 3) Server A furthermore has a non-clustered SFSB too.
     * 4) This test invokes on the non-clustered SFSB on server A which inturn invokes on the clustered
     * SFSB on server B (this is done by disabling local ejb receiver in the jboss-ejb-client.xml).
     * 5) Invocation works fine and at the same time (asynchronously) server B sends back a cluster topology
     * to server A containing, both server A and B and the nodes.
     * 6) The test then stops server B and invokes on the same non-clustered SFSB instance of server A, which inturn
     * invokes on the stateful clustered SFSB which it had injected earlier. This time since server B (which owns the session)
     * is down the invocation is expected to end up on server A itself (since server A is part of the cluster too)
     *
     * @throws Exception
     */
    @Test
    public void testServerToServerClusterFormation() throws Exception {
        // First start the default server
        this.container.start(DEFAULT_JBOSSAS);
        try {
            // deploy a deployment which contains a clustered EJB
            this.deployer.deploy(DEFAULT_AS_DEPLOYMENT);

            // start the other server
            this.container.start(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            // deploy the deployment containing a non-clustered EJB
            this.deployer.deploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);

            // invoke on the non-clustered bean which internally calls the clustered bean on a remote server
            final NodeNameEcho nonClusteredBean = (NodeNameEcho) context.lookup("ejb:/" + MODULE_NAME + "//" + NonClusteredStatefulNodeNameEcho.class.getSimpleName() + "!" + NodeNameEcho.class.getName() + "?stateful");
            final String nodeNameBeforeShutdown = nonClusteredBean.getNodeName(true);
            Assert.assertEquals("EJB invocation ended up on unexpected node", DEFAULT_JBOSSAS_NODE_NAME, nodeNameBeforeShutdown);

            // now shutdown the default server
            this.container.stop(DEFAULT_JBOSSAS);

            // now invoke again. this time the internal invocation on the clustered bean should end up on
            // one of the cluster nodes instead of the default server, since it was shutdown
            final String nodeNameAfterShutdown = nonClusteredBean.getNodeName(false);
            Assert.assertEquals("EJB invocation ended up on unexpected node after shutdown", JBOSSAS_WITH_OUTBOUND_CONNECTION_NODE_NAME, nodeNameAfterShutdown);

        } finally {
            try {
                this.deployer.undeploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
                this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
            try {
                this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
                this.container.stop(DEFAULT_JBOSSAS);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
        }

    }

    /**
     * Sets up the EJB client context to use a selector which processes and sets up EJB receivers
     * based on this testcase specific jboss-ejb-client.properties file
     *
     * @return
     * @throws java.io.IOException
     */
    private static ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        // setup the selector
        final String clientPropertiesFile = "org/jboss/as/test/manualmode/ejb/client/cluster/jboss-ejb-client.properties";
        final InputStream inputStream = EJBClientClusterConfigurationTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);

        return EJBClientContext.setSelector(selector);
    }
}
