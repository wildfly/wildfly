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

package org.jboss.as.test.manualmode.web.valve;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This class tests a web descriptor valve definition and global valve defined in server conf.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebDescriptorValveTestCase {
    private static Logger log = Logger.getLogger(WebDescriptorValveTestCase.class);
    
    @ArquillianResource
    private static ContainerController container;
    
    @ArquillianResource
    private Deployer deployer;
    
    private static final String modulename = "org.jboss.testvalve";
    private static final String classname = TestValve.class.getName();
    private static final String baseModulePath = "/../modules/" + modulename.replace(".", "/") + "/main";
    private static final String jarName = "testvalve.jar";
    private static final String VALVE_NAME = "testvalve";
    private static final String PARAM_NAME = "testparam";
    private static final String WEB_PARAM_VALUE = "webdescriptor";
    private static final String GLOBAL_PARAM_VALUE = "global";
    private static final String DEPLOYMENT = "valve";

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-descriptor-valve-test.war");
        war.addClasses(HelloServlet.class);
        war.addAsWebInfResource(WebDescriptorValveTestCase.class.getPackage(),"jboss-web.xml", "jboss-web.xml");
        war.addAsManifestResource(WebDescriptorValveTestCase.class.getPackage(),"MANIFEST.MF", "MANIFEST.MF");
        return war;
    }
    
    @Test
    @InSequence(-1)
    public void startServer() throws Exception {
        container.start(GlobalValveTestCase.CONTAINER);
    }

    @Test
    @InSequence(0)
    public void createValveAndDeploy(@ArquillianResource ManagementClient client) throws Exception {
        // as first test in sequence creating valve module
        ValveUtil.createValveModule(client, modulename, baseModulePath, jarName);
        // valve is ready - let's deploy
        deployer.deploy(DEPLOYMENT);
    }
    
        
    @Test
    @InSequence(1)
    public void testWebDescriptor(@ArquillianResource URL url) throws Exception {        
        log.debug("Testing url " + url + " against one valve defined in jboss-web.xml descriptor");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals(WEB_PARAM_VALUE, valveHeaders[0].getValue());
    }

    @Test
    @InSequence(3)
    public void addValve(@ArquillianResource ManagementClient client) throws Exception {
        Map<String,String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, GLOBAL_PARAM_VALUE);
        ValveUtil.addValve(client, VALVE_NAME, modulename, classname, params);
        container.stop(GlobalValveTestCase.CONTAINER);
        container.start(GlobalValveTestCase.CONTAINER);
        int i = 0;
        while(!client.isServerInRunningState() && ++i<10) {
            Thread.sleep(1000);
        }
        log.info("Server " + (client.isServerInRunningState() ? "is":"is not") + " running!");
    }
    
    @Test
    @InSequence(4)
    public void testValveOne(@ArquillianResource URL url) throws Exception {
        log.debug("Testing url " + url + " against two valves - one defined in web descriptor other is defined globally in server configuration");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There were two valves defined", 2, valveHeaders.length);
        assertEquals(GLOBAL_PARAM_VALUE, valveHeaders[0].getValue());
        assertEquals(WEB_PARAM_VALUE, valveHeaders[1].getValue());
    }
    
    
    @Test
    @InSequence(99)
    public void cleanUp(@ArquillianResource ManagementClient client) throws Exception {
        ValveUtil.removeValve(client, VALVE_NAME);
        deployer.undeploy(DEPLOYMENT);
        container.stop(GlobalValveTestCase.CONTAINER);
    }    
}