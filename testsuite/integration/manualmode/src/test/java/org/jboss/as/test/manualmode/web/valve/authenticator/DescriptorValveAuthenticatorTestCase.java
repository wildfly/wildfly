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
package org.jboss.as.test.manualmode.web.valve.authenticator;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.manualmode.web.valve.authenticator.AuthValveConstants.*;

/**
 * This class tests a global valve.
 *
 * @author Jean-Frederic Clere
 * @author Ondrej Chaloupka
 * @author Radim Hatlapatka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DescriptorValveAuthenticatorTestCase {

    private static Logger log = Logger.getLogger(DescriptorValveAuthenticatorTestCase.class);
    @ArquillianResource
    private static ContainerController container;
    @ArquillianResource
    private Deployer deployer;
    
    private static final String DEPLOYMENT_NAME = "descriptorValveAuth";

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-descriptor-valve-test.war");
        war.addClasses(HelloServlet.class);
        war.addAsWebInfResource(DescriptorValveAuthenticatorTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(DescriptorValveAuthenticatorTestCase.class.getPackage(), "web-custom-auth.xml", "web.xml");
        war.addAsManifestResource(DescriptorValveAuthenticatorTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        return war;
    }

    @Test
    @InSequence(-1)
    public void startServer() throws Exception {
        container.start(CONTAINER);
    }

    @Test
    @InSequence(0)
    public void createValveAndDeploy(@ArquillianResource ManagementClient client) throws Exception {
        // as first test in sequence creating valve module
        ValveUtil.createValveModule(client, MODULENAME, getBaseModulePath(MODULENAME), AUTH_VALVE_JAR, AUTHENTICATOR);

        // authenticator valve is ready - let's deploy
        deployer.deploy(DEPLOYMENT_NAME);
    }

    @Test
    @InSequence(1)
    public void testWebDescriptor(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        String appUrl = url.toExternalForm() + WEB_APP_URL;
        log.debug("Testing url " + appUrl + " against one authenticator valve defined in jboss-web.xml descriptor");
        Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl));
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals(WEB_PARAM_VALUE, valveHeaders[0].getValue());
    }

    @Test
    @InSequence(2)
    public void testValveGlobal(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        // adding authenticator valve based on the created module
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, GLOBAL_PARAM_VALUE);        
        ValveUtil.addValve(client, CUSTOM_AUTHENTICATOR_1, MODULENAME, AUTHENTICATOR.getName(), params);        
        ValveUtil.reload(client);

        String appUrl = url.toExternalForm() + WEB_APP_URL;
        log.debug("Testing url " + appUrl + " against two authenticators - one defined in web descriptor other is defined globally in server configuration and checking which one was used");
        Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl));
        assertEquals("There were two valves defined (but detected these valve headers: " + Arrays.toString(valveHeaders) + ")", 1, valveHeaders.length);
        assertEquals(WEB_PARAM_VALUE, valveHeaders[0].getValue()); // testing that it is used authenticator defined in web descriptors valve
    }

    @Test
    @InSequence(99)
    public void cleanUp(@ArquillianResource ManagementClient client) throws Exception {
        deployer.undeploy(DEPLOYMENT_NAME);
        ValveUtil.removeValve(client, CUSTOM_AUTHENTICATOR_1);
        container.stop(CONTAINER);
    }
}