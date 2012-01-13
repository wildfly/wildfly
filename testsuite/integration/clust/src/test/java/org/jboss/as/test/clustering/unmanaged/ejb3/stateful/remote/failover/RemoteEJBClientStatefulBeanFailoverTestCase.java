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

package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover;

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
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteEJBClientStatefulBeanFailoverTestCase {

    private static final Logger logger = Logger.getLogger(RemoteEJBClientStatefulBeanFailoverTestCase.class);

    private static final String MODULE_NAME = "remote-ejb-client-stateful-bean-failover-test";
    public static final String CONTAINER1 = "clustering-udp-0-unmanaged";
    public static final String CONTAINER2 = "clustering-udp-1-unmanaged";
    private static final String ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER1 = "container1-deployment";
    private static final String ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER2 = "container2-deployment";

    private static Context context;

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;


    @Deployment(name = ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER1, managed = false, testable = false)
    @TargetsContainer(CONTAINER1)
    public static Archive createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER2, managed = false, testable = false)
    @TargetsContainer(CONTAINER2)
    public static Archive createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(CounterBean.class.getPackage());
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);

    }

    @Test
    public void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception {
        // Container is unmanaged, so start it ourselves
        this.container.start(CONTAINER1);
        // deploy to container1
        this.deployer.deploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER1);

        // start the other container too
        this.container.start(CONTAINER2);
        this.deployer.deploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER2);

        final ContextSelector<EJBClientContext> previousSelector = this.setupEJBClientContextSelector();
        final String jndiName = "ejb:" + "" + "/" + MODULE_NAME + "/" + "" + "/" + CounterBean.class.getSimpleName() + "!" + RemoteCounter.class.getName() + "?stateful";
        boolean container1Stopped = false;
        boolean container2Stopped = false;
        try {
            final RemoteCounter remoteCounter = (RemoteCounter) context.lookup(jndiName);
            final int NUM_TIMES = 25;
            for (int i = 0; i < NUM_TIMES; i++) {
                final CounterResult result = remoteCounter.increment();
                logger.info("Counter incremented to " + result.getCount() + " on node " + result.getNodeName());
            }
            final CounterResult result = remoteCounter.getCount();
            Assert.assertNotNull("Result from remote stateful counter was null", result);
            Assert.assertEquals("Unexpected count from remote counter", NUM_TIMES, result.getCount());

            // shutdown the node on which the previous invocation happened
            final int totalCountBeforeShuttingDownANode = result.getCount();
            final String previousInvocationNodeName = result.getNodeName();
            // the value is configured in arquillian.xml of the project
            if (previousInvocationNodeName.equals("node-udp-0")) {
                this.deployer.undeploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER1);
                this.container.stop(CONTAINER1);
                container1Stopped = true;
            } else {
                this.deployer.undeploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER2);
                this.container.stop(CONTAINER2);
                container2Stopped = true;
            }
            // invoke again
            CounterResult resultAfterShuttingDownANode = remoteCounter.increment();
            Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", resultAfterShuttingDownANode);
            Assert.assertEquals("Unexpected count from remote counter, after shutting down a node", totalCountBeforeShuttingDownANode + 1, resultAfterShuttingDownANode.getCount());
            Assert.assertFalse("Result was received from an unexpected node, after shutting down a node", previousInvocationNodeName.equals(resultAfterShuttingDownANode.getNodeName()));

            // repeat invocations
            final int countBeforeDecrementing = resultAfterShuttingDownANode.getCount();
            final String aliveNode = resultAfterShuttingDownANode.getNodeName();
            for (int i = NUM_TIMES; i > 0; i--) {
                resultAfterShuttingDownANode = remoteCounter.decrement();
                Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", resultAfterShuttingDownANode);
                Assert.assertEquals("Result was received from an unexpected node, after shutting down a node", aliveNode, resultAfterShuttingDownANode.getNodeName());
                logger.info("Counter decremented to " + resultAfterShuttingDownANode.getCount() + " on node " + resultAfterShuttingDownANode.getNodeName());
            }
            final CounterResult finalResult = remoteCounter.getCount();
            Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", finalResult);
            final int finalCount = finalResult.getCount();
            final String finalNodeName = finalResult.getNodeName();
            Assert.assertEquals("Result was received from an unexpected node, after shutting down a node", aliveNode, finalNodeName);
            Assert.assertEquals("Unexpected count from remote counter, after shutting down a node", countBeforeDecrementing - NUM_TIMES, finalCount);
        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
            // shutdown the containers
            if (!container1Stopped) {
                this.deployer.undeploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER1);
                this.container.stop(CONTAINER1);
            }

            if (!container2Stopped) {
                this.deployer.undeploy(ARQUILLIAN_DEPLOYMENT_NAME_FOR_CONTAINER2);
                this.container.stop(CONTAINER2);
            }
        }
    }

    private ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        // setup the selector
        final String clientPropertiesFile = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
        final InputStream inputStream = RemoteEJBClientStatefulBeanFailoverTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
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
