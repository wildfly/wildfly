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

package org.jboss.as.test.manualmode.ejb.shutdown;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations over a remote interface still work even while the container is shutting down.
 *
 * @author Stuart Douglas
 * @see https://issues.jboss.org/browse/AS7-4162
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteCallWhileShuttingDownTestCase {

    private static final Logger logger = Logger.getLogger(RemoteCallWhileShuttingDownTestCase.class);

    public static final String DEP1 = "dep1";
    public static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private Context context;
    private ContextSelector<EJBClientContext> previousClientContextSelector;

    @Before
    public void before() throws Exception {
        this.context = Util.createNamingContext();
        // setup the client context selector
        this.previousClientContextSelector = setupEJBClientContextSelector();

    }

    @After
    public void after() throws NamingException {
        if (this.previousClientContextSelector != null) {
            EJBClientContext.setSelector(this.previousClientContextSelector);
        }
        this.context.close();
    }

    @Deployment(name = DEP1, managed = false, testable = false)
    public static Archive createContainer1Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, DEP1 + ".jar");
        ejbJar.addClasses(ShutdownBean.class, RemoteEcho.class, RealEcho.class, LatchBean.class, RemoteLatch.class);
        return ejbJar;
    }

    /**
     * Deploys the deployments to the server, then starts a shutdown sequence.
     * <p/>
     * Makes sure that remote calls still succeed on the singleton until it is shut down.
     *
     * @throws Exception
     */
    @Test
    public void testServerShutdownRemoteCall() throws Exception {
        // First start the server which has a remote-outbound-connection
        this.container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        try {
            this.deployer.deploy(DEP1);

            RemoteLatch latch = (RemoteLatch) context.lookup("ejb:/dep1/LatchBean!" + RemoteLatch.class.getName());

            //now we need to shutdown the container
            //but we need to do it asynchronously

            final ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.ADDRESS);
            op.get(ModelDescriptionConstants.OP_ADDR).set(new ModelNode());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.SHUTDOWN);
            ManagementOperations.executeOperation(client, op);
            //we sleep here to increase the chance of the container shutting down a service that we need
            //to make it more likely to pick up a dependency problem
            Thread.sleep(1000);
            Assert.assertEquals("Real hello", latch.getEchoMessage());
            // we don't want to "wait" for a response since the server is in shutdown mode and waiting for a result can result in intermittent connection/channel close issues
            // where when the client has sent a request and is waiting for a server response and the server in the meantime closed the channel.
            // So create an asynchronous proxy and invoke the "void" method which acts as fully asynchronous by *not* waiting for a response from the server. Kind of fire and forget.
            // see https://issues.jboss.org/browse/WFLY-1532 for more details.
            final RemoteLatch asynchronousProxy = EJBClient.asynchronous(latch);
            asynchronousProxy.testDone();

            while (managementClient.isServerInRunningState()) {
                Thread.sleep(50);
            }

        } finally {
            try {
                if (!managementClient.isServerInRunningState()) {
                    container.start(CONTAINER);
                }
                this.deployer.undeploy(DEP1);
                this.container.stop(CONTAINER);
            } catch (Exception e) {
                logger.warn("Exception during container shutdown", e);
            }
            client.close();
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
        final String clientPropertiesFile = "jboss-ejb-client.properties";
        final InputStream inputStream = RemoteCallWhileShuttingDownTestCase.class.getResourceAsStream(clientPropertiesFile);
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
