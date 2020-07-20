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

package org.jboss.as.test.manualmode.ejb.client.outbound.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.manualmode.ejb.Util;
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
 * Tests that an EJB client context containing a reference to a remote outbound connection, has the ability to
 * reconnect a failed connection
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-3820 for details
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteOutboundConnectionReconnectTestCase {

    private static final Logger logger = Logger.getLogger(RemoteOutboundConnectionReconnectTestCase.class);

    private static final String SERVER_ONE_MODULE_NAME = "server-one-module";
    private static final String SERVER_TWO_MODULE_NAME = "server-two-module";

    private static final String JBOSSAS_NON_CLUSTERED = "jbossas-non-clustered";
    private static final String JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED = "jbossas-with-remote-outbound-connection-non-clustered";

    private static final String DEFAULT_AS_DEPLOYMENT = "default-jbossas-deployment";
    private static final String DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML = "other-deployment";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private Context context;

    @Before
    public void before() throws Exception {
        final Properties ejbClientProperties = setupEJBClientProperties();
        this.context = Util.createNamingContext(ejbClientProperties);
    }

    @After
    public void after() throws NamingException {
        this.context.close();
    }

    @Deployment(name = DEFAULT_AS_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(JBOSSAS_NON_CLUSTERED)
    public static Archive<?> createContainer1Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SERVER_TWO_MODULE_NAME + ".jar");
        ejbJar.addClasses(EchoOnServerTwo.class, RemoteEcho.class);
        return ejbJar;
    }

    @Deployment(name = DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML, managed = false, testable = false)
    @TargetsContainer(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED)
    public static Archive<?> createContainer2Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SERVER_ONE_MODULE_NAME + ".jar");
        ejbJar.addClasses(EchoOnServerOne.class, RemoteEcho.class, IndependentBean.class);
        ejbJar.addAsManifestResource(EchoOnServerOne.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbJar;
    }

    /**
     * Start a server (A) which has a remote outbound connection to another server (B). Server (B) is down.
     * Deploy (X) to server A. X contains a jboss-ejb-client.xml pointing to server B (which is down). The deployment
     * must succeed. However invocations on beans which depend on server B should fail.
     * Then start server B and deploy Y to it. Invoke again on server A beans which depend on server B and this time
     * they should pass
     *
     * @throws Exception
     */
    @Test
    public void testRemoteServerStartsLate() throws Exception {
        // First start the server which has a remote-outbound-connection
        this.container.start(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED);
        boolean defaultContainerStarted = false;
        try {
            // deploy a deployment which contains jboss-ejb-client.xml that contains an EJB receiver pointing
            // to a server which hasn't yet started. Should succeed without throwing deployment error
            this.deployer.deploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
            // To make sure deployment succeeded and invocations are possible, call an independent bean
            final RemoteEcho independentBean = (RemoteEcho) context.lookup("ejb:/" + SERVER_ONE_MODULE_NAME + "//" + IndependentBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            final String msg = "Hellooooo!";
            final String echoFromIndependentBean = independentBean.echo(msg);
            Assert.assertEquals("Unexpected echo from independent bean", msg, echoFromIndependentBean);

            // now try invoking the EJB (which calls a delegate bean on other server) on this server.
            // should fail with no EJB receivers, since the other server
            // which can handle the delegate bean invocation hasn't yet started.
            try {
                final RemoteEcho dependentBean = (RemoteEcho) context.lookup("ejb:/" + SERVER_ONE_MODULE_NAME + "//" + EchoOnServerOne.class.getSimpleName() + "!" + RemoteEcho.class.getName());
                final String echoBeforeOtherServerStart = dependentBean.echo(msg);
                Assert.fail("Invocation on bean when was expected to fail due to other server being down");
            } catch (Exception e) {
                // expected
                logger.trace("Got the expected exception on invoking a bean when other server was down", e);
            }
            // now start the main server
            this.container.start(JBOSSAS_NON_CLUSTERED);
            defaultContainerStarted = true;
            // deploy to this container
            this.deployer.deploy(DEFAULT_AS_DEPLOYMENT);

            // now invoke the EJB (which had failed earlier)
            final RemoteEcho dependentBean = (RemoteEcho) context.lookup("ejb:/" + SERVER_ONE_MODULE_NAME + "//" + EchoOnServerOne.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            final String echoAfterOtherServerStarted = dependentBean.echo(msg);
            Assert.assertEquals("Unexpected echo from bean", EchoOnServerTwo.ECHO_PREFIX + msg, echoAfterOtherServerStarted);

        } finally {
            try {
                this.deployer.undeploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
                this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
            if (defaultContainerStarted) {
                try {
                    this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
                    this.container.stop(JBOSSAS_NON_CLUSTERED);
                } catch (Exception e) {
                    logger.debug("Exception during container shutdown", e);
                }
            }
        }
    }

    /**
     * Start a server (A) which has a remote outbound connection to another server (B). Also start Server (B).
     * Deploy (X) to server A. X contains a jboss-ejb-client.xml pointing to server B. The deployment and invocations
     * must succeed.
     * Now stop server (B). Invoke again on the bean. Invocation should fail since server B is down. Now
     * restart server B and invoke again on the bean. Invocation should pass since the EJB client context is
     * expected to reconnect to the restarted server B.
     *
     * @throws Exception
     */
    @Test
    public void testRemoteServerRestarts() throws Exception {
        // Start the main server
        this.container.start(JBOSSAS_NON_CLUSTERED);
        // deploy to this container
        this.deployer.deploy(DEFAULT_AS_DEPLOYMENT);

        // Now start the server which has a remote-outbound-connection
        this.container.start(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED);
        this.deployer.deploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);

        boolean defaultContainerStarted = true;
        try {
            // To make sure deployment succeeded and invocations are possible, call an independent bean
            final RemoteEcho independentBean = (RemoteEcho) context.lookup("ejb:/" + SERVER_ONE_MODULE_NAME + "//" + IndependentBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            final String msg = "Hellooooo!";
            final String echoFromIndependentBean = independentBean.echo(msg);
            Assert.assertEquals("Unexpected echo from independent bean", msg, echoFromIndependentBean);

            // now try invoking the EJB (which calls a delegate bean on other server) on this server.
            // should fail with no EJB receivers, since the other server
            // which can handle the delegate bean invocation hasn't yet started.
            final RemoteEcho dependentBean = (RemoteEcho) context.lookup("ejb:/" + SERVER_ONE_MODULE_NAME + "//" + EchoOnServerOne.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            final String echoBeforeShuttingDownServer = dependentBean.echo(msg);
            Assert.assertEquals("Unexpected echo from bean", EchoOnServerTwo.ECHO_PREFIX + msg, echoBeforeShuttingDownServer);

            // now stop the main server
            this.container.stop(JBOSSAS_NON_CLUSTERED);
            defaultContainerStarted = false;

            try {
                final String echoAfterServerShutdown = dependentBean.echo(msg);
                Assert.fail("Invocation on bean when was expected to fail due to other server being down");
            } catch (Exception e) {
                // expected
                logger.trace("Got the expected exception on invoking a bean when other server was down", e);
            }

            // now restart the main server
            this.container.start(JBOSSAS_NON_CLUSTERED);
            defaultContainerStarted = true;

            final String echoAfterServerRestart = dependentBean.echo(msg);
            Assert.assertEquals("Unexpected echo from bean after server restart", EchoOnServerTwo.ECHO_PREFIX + msg, echoAfterServerRestart);


        } finally {
            try {
                this.deployer.undeploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
                this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION_NON_CLUSTERED);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
            if (defaultContainerStarted) {
                try {
                    this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
                    this.container.stop(JBOSSAS_NON_CLUSTERED);
                } catch (Exception e) {
                    logger.debug("Exception during container shutdown", e);
                }
            }
        }

    }

    /**
     * Sets up the EJB client properties based on this testcase specific jboss-ejb-client.properties file
     *
     * @return
     * @throws java.io.IOException
     */
    private static Properties setupEJBClientProperties() throws IOException {
        // setup the properties
        final String clientPropertiesFile = "org/jboss/as/test/manualmode/ejb/client/outbound/connection/jboss-ejb-client.properties";
        final InputStream inputStream = RemoteOutboundConnectionReconnectTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
