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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
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
public class GlobalAuthenticatorTestCase {

    private static Logger log = Logger.getLogger(GlobalAuthenticatorTestCase.class);
    @ArquillianResource
    private static ContainerController container;
    @ArquillianResource
    private Deployer deployer;
    private static final String DEPLOYMENT_NAME = "valveAuth";
    private static final String DEPLOYMENT_NAME_2 = "valveAuth2";

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "global-authvalve-test.war");
        war.addClasses(HelloServlet.class);
        war.addAsWebInfResource(GlobalAuthenticatorTestCase.class.getPackage(), "web-custom-auth.xml", "web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT_NAME_2, managed = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive Hello2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "global-authvalve-second-test.war");
        war.addClasses(HelloServlet.class);
        war.addAsWebInfResource(GlobalAuthenticatorTestCase.class.getPackage(), "web-global-auth.xml", "web.xml");
//        war.addAsManifestResource(GlobalAuthenticatorTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        return war;
    }

    @Test
    @InSequence(-1)
    public void startServer() throws Exception {
        container.start(CONTAINER);
    }

    @Test
    @InSequence(0)
    @OperateOnDeployment(value = DEPLOYMENT_NAME)
    public void createValveAndDeploy(@ArquillianResource ManagementClient client) throws Exception {
        // as first test in sequence creating valve module
        ValveUtil.createValveModule(client, MODULENAME, getBaseModulePath(MODULENAME), AUTH_VALVE_JAR, AUTHENTICATOR);
        // adding valve based on the created module
        ValveUtil.addValve(client, CUSTOM_AUTHENTICATOR_1, MODULENAME, AUTHENTICATOR.getName(), null);

        ValveUtil.reload(client);

        // valve is ready - let's deploy
        deployer.deploy(DEPLOYMENT_NAME);
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment(value = DEPLOYMENT_NAME_2)
    public void createSecondValveAndDeploy(@ArquillianResource ManagementClient client) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, GLOBAL_PARAM_VALUE);
        ValveUtil.addValve(client, CUSTOM_AUTHENTICATOR_2, MODULENAME, AUTHENTICATOR.getName(), params);
        ValveUtil.reload(client);

        // valve is ready - let's deploy
        deployer.deploy(DEPLOYMENT_NAME_2);
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment(value = DEPLOYMENT_NAME)
    public void testValveAuthOne(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        String appUrl = url.toExternalForm() + WEB_APP_URL_1;
        log.debug("Testing url " + appUrl + " against one global valve authenticator named " + CUSTOM_AUTHENTICATOR_1);
        Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl));
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals("One valve with not defined param expecting default param value", DEFAULT_PARAM_VALUE, valveHeaders[0].getValue());
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment(value = DEPLOYMENT_NAME_2)
    public void testValveAuthTwo(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        String appUrl = url.toExternalForm() + WEB_APP_URL_2;
        log.debug("Testing url " + appUrl + " against one global valve authenticator named " + CUSTOM_AUTHENTICATOR_2);
        Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl));
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals("One valve with not defined param expecting default param value", GLOBAL_PARAM_VALUE, valveHeaders[0].getValue());
    }

    /**
     * Testing that if authenticator valve is disabled then it is not used
     */
    @Test
    @InSequence(10)
    @OperateOnDeployment(value = DEPLOYMENT_NAME_2)
    public void testValveAuthDisable(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        ValveUtil.activateValve(client, CUSTOM_AUTHENTICATOR_2, false);
        ValveUtil.reload(client);
        String appUrl = url.toExternalForm() + WEB_APP_URL_2;
        Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl), HttpURLConnection.HTTP_NOT_FOUND);
        assertEquals("Auth valve is disabled => expecting no valve headers: " + Arrays.toString(valveHeaders), 0, valveHeaders.length);
    }

    // test scenario when there is a standard valve + authenticator valve
    // TODO: not yet functional, needs to be finished. It has problem to find a module
    @Test
    @InSequence(20) // put here in order to prevent potential influence for other tests by the standard global valve
    @OperateOnDeployment(value = DEPLOYMENT_NAME)
    public void authWithStandardValve(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        // as first test in sequence creating valve module
        ValveUtil.createValveModule(client, STANDARD_VALVE_MODULE, getBaseModulePath(STANDARD_VALVE_MODULE), STANDARD_VALVE_JAR, SimpleValve.class);

        // adding valve based on the created module
        ValveUtil.addValve(client, STANDARD_VALVE, STANDARD_VALVE_MODULE, SimpleValve.class.getName(), null);

        ValveUtil.reload(client);
        try {
            String appUrl = url.toExternalForm() + WEB_APP_URL_1;
            log.debug("Testing url " + appUrl + " against two valves defined, one standard valve and one global valve authenticator named " + CUSTOM_AUTHENTICATOR_1);
            Header[] valveHeaders = ValveUtil.hitValve(new URL(appUrl));
            assertEquals("There was one valve defined - it's missing now", 2, valveHeaders.length);
//         TODO: test if valve headers contain correct values
            assertEquals("Standard simple valve with not defined param expecting default param value", STANDARD_VALVE_DEFAULT_PARAM_VALUE, valveHeaders[0].getValue());
            assertEquals("Authenticator valve with not defined param expecting default param value", DEFAULT_PARAM_VALUE, valveHeaders[1].getValue());
        } finally {
            ValveUtil.removeValve(client, STANDARD_VALVE);
        }
    }

    @Test
    @InSequence(99)
    @OperateOnDeployment(value = DEPLOYMENT_NAME)
    public void cleanUp(@ArquillianResource ManagementClient client) throws Exception {
        deployer.undeploy(DEPLOYMENT_NAME);
        deployer.undeploy(DEPLOYMENT_NAME_2);
        ValveUtil.removeValve(client, CUSTOM_AUTHENTICATOR_1);
        ValveUtil.removeValve(client, CUSTOM_AUTHENTICATOR_2);
        container.stop(CONTAINER);
    }
}