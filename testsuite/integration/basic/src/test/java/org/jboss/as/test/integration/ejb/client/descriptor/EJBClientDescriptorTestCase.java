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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ejb.EJBException;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * Tests that deployments containing a jboss-ejb-client.xml are processed correctly for EJB client context
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EJBClientDescriptorTestCase.EJBClientDescriptorTestCaseSetup.class)
public class EJBClientDescriptorTestCase {

    private static final Logger logger = Logger.getLogger(EJBClientDescriptorTestCase.class);

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";

    private static final String MODULE_NAME_ONE = "ejb-client-descriptor-test";
    private static final String MODULE_NAME_TWO = "ejb-client-descriptor-with-no-receiver-test";
    private static final String MODULE_NAME_THREE = "ejb-client-descriptor-with-local-and-remote-receivers-test";
    private static final String JBOSS_EJB_CLIENT_1_2_MODULE_NAME = "ejb-client-descriptor-test-with-jboss-ejb-client_1_2_xml";
    private static final String JBOSS_EJB_CLIENT_WITH_PROPERTIES_1_2_MODULE_NAME = "ejb-client-descriptor-test-with-jboss-ejb-client_with_properties_1_2_xml";


    private static final String outboundSocketName = "ejb-client-descriptor-test-outbound-socket";
    private static final String outboundConnectionName = "ejb-client-descriptor-test-remote-outbound-connection";

    static class EJBClientDescriptorTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final String socketBindingRef = "http";
            EJBManagementUtil.createLocalOutboundSocket(managementClient.getControllerClient(), "standard-sockets", outboundSocketName, socketBindingRef, Authentication.getCallbackHandler());
            logger.trace("Created local outbound socket " + outboundSocketName);

            final Map<String, String> connectionCreationOptions = new HashMap<String, String>();
            logger.trace("Creatng remote outbound connection " + outboundConnectionName);
            EJBManagementUtil.createRemoteOutboundConnection(managementClient.getControllerClient(), outboundConnectionName, outboundSocketName, connectionCreationOptions, Authentication.getCallbackHandler());
            logger.trace("Created remote outbound connection " + outboundConnectionName);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            EJBManagementUtil.removeRemoteOutboundConnection(managementClient.getControllerClient(), outboundConnectionName, Authentication.getCallbackHandler());
            logger.trace("Removed remote outbound connection " + outboundConnectionName);
            ServerReload.reloadIfRequired(managementClient.getControllerClient());

            EJBManagementUtil.removeLocalOutboundSocket(managementClient.getControllerClient(), "standard-sockets", outboundSocketName, Authentication.getCallbackHandler());
            logger.trace("Removed local outbound socket " + outboundSocketName);
        }
    }

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private Context context;

    @Deployment(name = "good-client-config", testable = false, managed = false)
    public static JavaArchive createDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_ONE + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "jboss-ejb-client_1_2_version", testable = false, managed = false)
    public static JavaArchive createJBossEJBClient12VersionDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JBOSS_EJB_CLIENT_1_2_MODULE_NAME + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "jboss-ejb-client_1_2.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "no-ejb-receiver-client-config", testable = false, managed = false)
    public static JavaArchive createNoEJBReceiverConfigDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_TWO + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "no-ejb-receiver-jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "local-and-remote-receviers-config", testable = false, managed = false)
    public static JavaArchive createLocalAndRemoteReceiverConfigDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_THREE + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "local-and-remote-receiver-jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Deployment(name = "jboss-ejb-client_with_properties_1_2_version", testable = false, managed = false)
    public static JavaArchive createJBossEJBClientWithProperties12VersionDeployment() throws Exception {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JBOSS_EJB_CLIENT_WITH_PROPERTIES_1_2_MODULE_NAME + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "jboss-ejb-client_with_properties_1_2.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "jboss-ejb-client_with_properties_1_2.properties", "jboss.properties");

        return jar;
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
            final String msg = "Hello world from an EJB client descriptor test!!!";
            final String echo = remoteEcho.echo(MODULE_NAME_ONE, msg);
            logger.trace("Received echo " + echo);
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
            final String msg = "Hello world from an EJB client descriptor test!!!";
            try {
                remoteEcho.echo(MODULE_NAME_TWO, msg);
                Assert.fail("Exepcted to fail due to no EJB receivers availability");
            } catch (EJBException e) {
                // no EJB receivers available, so expected to fail
                logger.trace("Received the expected exception during testing with no EJB receivers", e);
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
            final String msg = "Hello world from an EJB client descriptor test!!!";
            final String echo = remoteEcho.echo(MODULE_NAME_THREE, msg);
            logger.trace("Received echo " + echo);
            Assert.assertEquals("Unexpected echo returned from remote bean", msg, echo);
        } finally {
            deployer.undeploy("local-and-remote-receviers-config");
        }
    }

    /**
     * Invokes a method which takes longer than the configured invocation timeout. The client is expected
     * to a receive a timeout exception
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("jboss-ejb-client_1_2_version")
    public void testClientInvocationTimeout() throws Exception {
        deployer.deploy("jboss-ejb-client_1_2_version");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + JBOSS_EJB_CLIENT_1_2_MODULE_NAME + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from an EJB client descriptor test!!!";
            try {
                remoteEcho.twoSecondEcho(JBOSS_EJB_CLIENT_1_2_MODULE_NAME, msg);
                Assert.fail("Expected to receive a timeout for the invocation");
            } catch (Exception e) {
                if (e instanceof EJBException && e.getCause() instanceof TimeoutException) {
                    logger.trace("Got the expected timeout exception", e.getCause());
                    return;
                }
                throw e;
            }

        } finally {
            deployer.undeploy("jboss-ejb-client_1_2_version");
        }
    }

    /**
     * Tests that a deployment containing jboss-ejb-client.xml with remoting EJB receivers configured and property placeholders,
     * works as expected.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("jboss-ejb-client_with_properties_1_2_version")
    public void testClientPropertiesReplacementInConfig() throws Exception {
        deployer.deploy("jboss-ejb-client_with_properties_1_2_version");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + JBOSS_EJB_CLIENT_WITH_PROPERTIES_1_2_MODULE_NAME + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from an EJB client descriptor test!!!";
            remoteEcho.echo(JBOSS_EJB_CLIENT_WITH_PROPERTIES_1_2_MODULE_NAME, msg);
        } finally {
            deployer.undeploy("jboss-ejb-client_with_properties_1_2_version");
        }
    }


}
