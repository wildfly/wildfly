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
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LazyOutboundConnectionReconnectTestCase {

    private static final Logger logger = Logger.getLogger(LazyOutboundConnectionReconnectTestCase.class);

    private static final String SERVER_ONE_MODULE_NAME = "server-one-module";
    private static final String SERVER_TWO_MODULE_NAME = "server-two-module";

    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION = "jbossas-with-remote-outbound-connection";

    private static final String DEFAULT_AS_DEPLOYMENT = "default-jbossas-deployment";
    private static final String DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML = "other-deployment";

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
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SERVER_TWO_MODULE_NAME + ".jar");
        ejbJar.addClasses(EchoOnServerTwo.class, RemoteEcho.class);
        return ejbJar;
    }

    @Deployment(name = DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML, managed = false, testable = false)
    @TargetsContainer(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION)
    public static Archive createContainer2Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SERVER_ONE_MODULE_NAME + ".jar");
        ejbJar.addClasses(EchoOnServerOne.class, RemoteEcho.class, IndependentBean.class);
        ejbJar.addAsManifestResource(EchoOnServerOne.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbJar;
    }

    @Test
    public void testRemoteServerStartsLate() throws Exception {
        // First start the server which has a remote-outbound-connection
        this.container.start(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
        boolean defaultContainerStarted = false;
        try {
            // deploy a deployment which contains jboss-ejb-client.xml that contains a EJB receiver pointing
            // to a server which hasn't yet started. Should succeed without throwing deployment error
            this.deployer.deploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
            // To make sure deployment succeeded and invocations are possible, call a independent bean
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
                logger.info("Got the expected exception on invoking a bean when other server was down", e);
            }
            // now start the main server
            this.container.start(DEFAULT_JBOSSAS);
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
                this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
            if (defaultContainerStarted) {
                try {
                    this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
                    this.container.stop(DEFAULT_JBOSSAS);
                } catch (Exception e) {
                    logger.debug("Exception during container shutdown", e);
                }
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
        final String clientPropertiesFile = "org/jboss/as/test/manualmode/ejb/client/outbound/connection/jboss-ejb-client.properties";
        final InputStream inputStream = LazyOutboundConnectionReconnectTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
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
