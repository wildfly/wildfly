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

package org.jboss.as.test.integration.ejb.client.descriptor;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Tests that deployments containing a jboss-ejb-client.xml are processed correctly for EJB client context
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientDescriptorTestCase {

    private static final Logger logger = Logger.getLogger(EJBClientDescriptorTestCase.class);

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";

    private static final String MODULE_NAME_ONE = "ejb-client-descriptor-test";
    private static final String MODULE_NAME_TWO = "ejb-client-descriptor-with-no-receiver-test";
    private static final String MODULE_NAME_THREE = "ejb-client-descriptor-with-local-and-remote-receivers-test";

    private static Context context;

    private static final String outboundSocketName = "ejb-client-descriptor-test-outbound-socket";
    private static final String outboundConnectionName = "ejb-client-descriptor-test-remote-outbound-connection";

    private static boolean outboundSocketCreated;
    private static boolean outboundConnectionCreated;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "good-client-config", testable = false, managed = false)
    public static Archive createDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_ONE + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource("ejb/client/descriptor/jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "no-ejb-receiver-client-config", testable = false, managed = false)
    public static Archive createNoEJBReceiverConfigDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_TWO + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource("ejb/client/descriptor/no-ejb-receiver-jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "local-and-remote-receviers-config", testable = false, managed = false)
    public static Archive createLocalAndRemoteReceiverConfigDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_THREE + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource("ejb/client/descriptor/local-and-remote-receiver-jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
        setupRemoteOutboundConnection();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (outboundSocketCreated) {
            EJBManagementUtil.removeLocalOutboundSocket("localhost", 9999, "standard-sockets", outboundSocketName, Authentication.getCallbackHandler());
            logger.info("Removed local outbound socket " + outboundSocketName);
        }
        if (outboundConnectionCreated) {
            EJBManagementUtil.removeRemoteOutboundConnection("localhost", 9999, outboundConnectionName, Authentication.getCallbackHandler());
            logger.info("Removed remote outbound connection " + outboundConnectionName);
        }
    }

    private static void setupRemoteOutboundConnection() throws Exception {
        if (!outboundSocketCreated) {
            final String socketBindingRef = "remoting";
            EJBManagementUtil.createLocalOutboundSocket("localhost", 9999, "standard-sockets", outboundSocketName, socketBindingRef, Authentication.getCallbackHandler());
            outboundSocketCreated = true;
            logger.info("Created local outbound socket " + outboundSocketName);
        }

        if (!outboundConnectionCreated) {
            final Map<String, String> connectionCreationOptions = new HashMap<String, String>();
            connectionCreationOptions.put("SSL_ENABLED", "false"); //without class name should also work
            connectionCreationOptions.put("org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
            logger.info("Creatng remote outbound connection " + outboundConnectionName);
            EJBManagementUtil.createRemoteOutboundConnection("localhost", 9999, outboundConnectionName, outboundSocketName, connectionCreationOptions, Authentication.getCallbackHandler());
            outboundConnectionCreated = true;
            logger.info("Created remote outbound connection " + outboundConnectionName);
        }
    }

    /**
     * Tests that a deployment containing jboss-ejb-client.xml with remoting EJB receivers configured, works as expected
     *
     * @throws Exception
     */
    @Test
    public void testEJBClientContextConfiguration() throws Exception {
        deployer.deploy("good-client-config");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME_ONE + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from a EJB client descriptor test!!!";
            final String echo = remoteEcho.echo(MODULE_NAME_ONE, msg);
            logger.info("Received echo " + echo);
            Assert.assertEquals("Unexpected echo returned from remote bean", msg, echo);
        } finally {
            deployer.undeploy("good-client-config");
        }
    }

    /**
     * Tests that a deployment with a jboss-ejb-client.xml with no EJB receivers configured, fails due to
     * non-availability of EJB receivers in the EJB client context
     *
     * @throws Exception
     */
    @Test
    public void testEJBClientContextWithNoReceiversConfiguration() throws Exception {
        deployer.deploy("no-ejb-receiver-client-config");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME_TWO + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from a EJB client descriptor test!!!";
            try {
                final String echo = remoteEcho.echo(MODULE_NAME_TWO, msg);
                Assert.fail("Exepcted to fail due to no EJB receivers availability");
            } catch (EJBException e) {
                // no EJB receivers available, so expected to fail
                logger.info("Received the expected exception during testing with no EJB receivers", e);
                // TODO: We could even narrow down into the exception to ensure we got the right exception.
                // But that's a bit brittle too since there's no guarantee in terms of API on what underlying
                // exception will be thrown for non-availability of EJB receivers.
            }
        } finally {
            deployer.undeploy("no-ejb-receiver-client-config");

        }
    }

    /**
     * Tests that a deployment containing jboss-ejb-client.xml with both local and remoting EJB receivers configured,
     * works as expected
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("local-and-remote-receviers-config")
    public void testLocalAndRemoteReceiversClientConfig() throws Exception {
        deployer.deploy("local-and-remote-receviers-config");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME_THREE + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from a EJB client descriptor test!!!";
            final String echo = remoteEcho.echo(MODULE_NAME_THREE, msg);
            logger.info("Received echo " + echo);
            Assert.assertEquals("Unexpected echo returned from remote bean", msg, echo);
        } finally {
            deployer.undeploy("local-and-remote-receviers-config");
        }
    }

}
