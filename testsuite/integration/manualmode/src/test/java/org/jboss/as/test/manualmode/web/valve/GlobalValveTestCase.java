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
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This class tests a global valve.
 *
 * @author Jean-Frederic Clere
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GlobalValveTestCase {
    private static Logger log = Logger.getLogger(GlobalValveTestCase.class);
    
    public static final String CONTAINER = "default-jbossas";
    
    @ArquillianResource
    private static ContainerController container;
    
    @ArquillianResource
    private Deployer deployer;
    
    private static final String modulename = "org.jboss.testvalve";
    private static final String classname = TestValve.class.getName();
    private static final String baseModulePath = "/../modules/" + modulename.replace(".", "/") + "/main";
    private static final String jarName = "testvalve.jar";
    private static final String VALVE_NAME_1 = "testvalve1";
    private static final String VALVE_NAME_2 = "testvalve2";
    private static final String PARAM_NAME = "testparam";
    /** the default value is hardcoded in {@link TestValve} */
    private static final String DEFAULT_PARAM_VALUE = "DEFAULT_VALUE";
    private static final String DEPLOYMENT = "valve";

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "global-valve-test.war");
        war.addClasses(HelloServlet.class);
        return war;
    }
    
    @Before
    public void before() throws Exception {
        container.start(CONTAINER);
        log.debug("Server started");
        deployer.deploy(DEPLOYMENT);
        log.debug("Deployed " + DEPLOYMENT);
    }

    @After
    public void after() throws Exception {
        try {
            deployer.undeploy(DEPLOYMENT);
            log.info("Undeployed " + DEPLOYMENT);
        } finally {
            container.stop(CONTAINER);
            log.info("Server stopped");
        }
    }


    @Test
    @InSequence(0)
    public void addValveOne(@ArquillianResource ManagementClient client) throws Exception {
        // as first test in sequence creating valve module
        ValveUtil.createValveModule(client, modulename, baseModulePath, jarName);
        // adding valve based on the created module
        ValveUtil.addValve(client, VALVE_NAME_1, modulename, classname, null);   
    }
    
    
    @Test
    @InSequence(1)
    public void testValveOne(@ArquillianResource URL url) throws Exception {
        log.debug("Testing url " + url + " against one global valve named " + VALVE_NAME_1);
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals("One valve with not defined param expecting default param value", DEFAULT_PARAM_VALUE, valveHeaders[0].getValue());
    }
    
    @Test
    @InSequence(2)
    public void addValveTwo(@ArquillianResource ManagementClient client) throws Exception {
        Map<String,String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, VALVE_NAME_2); //as param of valve defining its name
        ValveUtil.addValve(client, VALVE_NAME_2, modulename, classname, params);
    }

    @Test
    @InSequence(3)
    public void testValveTwo(@ArquillianResource URL url) throws Exception {        
        log.debug("Testing url " + url + " against two valves named " + VALVE_NAME_1 + " and " + VALVE_NAME_2);
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There were two global valves defined - they're missing now", 2, valveHeaders.length);
        // valve execution is not ordered - consulted with jfclere
        if(DEFAULT_PARAM_VALUE.equals(valveHeaders[0].getValue())) {
            assertEquals("First header has default parameter second has to have from param", VALVE_NAME_2, valveHeaders[1].getValue());
        }
        else if(VALVE_NAME_2.equals(valveHeaders[0].getValue())) {
            assertEquals("First header has parametrized value the second has to have default one", DEFAULT_PARAM_VALUE, valveHeaders[1].getValue());    
        } else {
            fail("The first header has value neither default nor parametrized. The value is: " + valveHeaders[0].getValue() + " what is not correct.");
        }
    }

    @Test
    @InSequence(4)
    public void passivateValve(@ArquillianResource ManagementClient client) throws Exception {
        ValveUtil.activateValve(client, VALVE_NAME_1, false);
    }
    
    @Test
    @InSequence(5)
    public void testValveDisabled(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {       
        log.debug("Testing url " + url + " against one global valve named " + VALVE_NAME_2 + ". The second one "+ VALVE_NAME_1 +" is disabled");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There is one active valve defined", 1, valveHeaders.length);
        assertEquals("Just second parametrized valve is active - defined param value is expected to be returned", VALVE_NAME_2, valveHeaders[0].getValue());
        
        // at the end removing valve
        ValveUtil.removeValve(client, VALVE_NAME_1);
        ValveUtil.removeValve(client, VALVE_NAME_2);
    }
}