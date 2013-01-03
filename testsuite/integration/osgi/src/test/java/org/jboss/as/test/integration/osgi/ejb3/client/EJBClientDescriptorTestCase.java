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

package org.jboss.as.test.integration.osgi.ejb3.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

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

    private static final String BUNDLE_NAME_ONE = "ejb-client-descriptor-bundle";

    private static final String outboundSocketName = "ejb-client-descriptor-test-outbound-socket";
    private static final String outboundConnectionName = "ejb-client-descriptor-test-remote-outbound-connection";

    static class EJBClientDescriptorTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final String socketBindingRef = "remoting";
            EJBManagementUtil.createLocalOutboundSocket(managementClient.getControllerClient(), "standard-sockets", outboundSocketName, socketBindingRef, Authentication.getCallbackHandler());
            logger.info("Created local outbound socket " + outboundSocketName);

            final Map<String, String> connectionCreationOptions = new HashMap<String, String>();
            logger.info("Creating remote outbound connection " + outboundConnectionName);
            EJBManagementUtil.createRemoteOutboundConnection(managementClient.getControllerClient(), outboundConnectionName, outboundSocketName, connectionCreationOptions, Authentication.getCallbackHandler());
            logger.info("Created remote outbound connection " + outboundConnectionName);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            EJBManagementUtil.removeLocalOutboundSocket(managementClient.getControllerClient(), "standard-sockets", outboundSocketName, Authentication.getCallbackHandler());
            logger.info("Removed local outbound socket " + outboundSocketName);

            EJBManagementUtil.removeRemoteOutboundConnection(managementClient.getControllerClient(), outboundConnectionName, Authentication.getCallbackHandler());
            logger.info("Removed remote outbound connection " + outboundConnectionName);
        }
    }

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private Context context;

    @Deployment(name = "good-client-config-bundle", testable = false, managed = false)
    public static JavaArchive createGoodClientConfigBundle() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, BUNDLE_NAME_ONE + ".jar");
        jar.addPackage(EchoBean.class.getPackage());
        jar.addAsManifestResource(EJBClientDescriptorTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        jar.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return jar;
    }


    /**
     * Tests that a {@link Bundle} deployment containing jboss-ejb-client.xml with remoting EJB receivers configured, works as expected
     * https://issues.jboss.org/browse/AS7-5009
     */
    @Test
    public void testEJBClientContextConfigurationInOSGiBundle() throws Exception {
        deployer.deploy("good-client-config-bundle");
        try {
            final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + BUNDLE_NAME_ONE + "/" + DISTINCT_NAME
                    + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
            final String msg = "Hello world from a EJB client descriptor test!!!";
            final String echo = remoteEcho.echo(BUNDLE_NAME_ONE, msg);
            logger.info("Received echo " + echo);
            Assert.assertEquals("Unexpected echo returned from remote bean", msg, echo);
        } finally {
            deployer.undeploy("good-client-config-bundle");
        }
    }



}
