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
package org.jboss.as.test.manualmode.classloading.leak;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.classloader.leak.AbstractClassloaderLeakTestBase;
import org.jboss.as.test.manualmode.ejb.client.reconnect.EJBClientReconnectionTestCase;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for classloader leak following deployment, usage and undeployment
 * of WAR application containing Servlet and JSP page. 
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class ClassloaderLeakWarTestCase extends AbstractClassloaderLeakTestBase {
            
    private static final Logger log = Logger.getLogger(ClassloaderLeakWarTestCase.class);
    
    @ArquillianResource
    private Deployer deployer;
        
    @ArquillianResource
    private ContainerController controller;
    
    private static final String DEPLOYMENT_APP_NAME = "ClassloaderLeakWarTestApp";
    private static final String CONTAINER = "default-jbossas";
    
    /*
     * Deployment containing the test case holding classloader reference.
     */
    @Deployment(name = DEPLOYMENT_DRIVER_ID, managed = false)
    public static Archive<?> getDeployment() {                      
        WebArchive war = prepareDriverWar();
        war.addClass(ClassloaderLeakWarTestCase.class);
        return war;
    }    

    /*
     * Deployment tested for classloader leak.
     */
    @Deployment(name = DEPLOYMENT_APP_ID, managed = false)
    public static Archive<?> getDeployment2() {
        WebArchive war = prepareTestAppWar(DEPLOYMENT_APP_NAME);
        // set dependency to the test deployment
        war.setManifest(new StringAsset(
                "Manifest-Version: 1.0" + NL + 
                "Dependencies: deployment." + DEPLOYMENT_DRIVER_NAME + ", org.jboss.modules" + NL)
                );
        return war;
    } 
        
    @Test
    @InSequence(-1)
    @RunAsClient
    public void startContainer() throws Exception {
        controller.start(CONTAINER);
        log.info("===appserver started===");
        deployer.deploy(DEPLOYMENT_DRIVER_ID);
        log.info("===deployment deployed===");
    }

    @Test
    @InSequence(1)
    @RunAsClient
    public void stopContainer() throws Exception {
        try {
            deployer.undeploy(DEPLOYMENT_DRIVER_ID);
            log.info("===deployment undeployed===");
        } finally {
            controller.stop(CONTAINER);
            log.info("===appserver stopped===");
        }
    }
    
    @Test
    @OperateOnDeployment(DEPLOYMENT_DRIVER_ID)
    public void testWarModuleRelease() throws Exception {
        
        assertClassloaderNotRegistered();
        
        // deploy war
        deployer.deploy(DEPLOYMENT_APP_ID);
        
        // check that the application classloader is registered
        assertClassloaderRegistered();
        
        // test war
        testWar(webURI + "/" + DEPLOYMENT_APP_NAME);
        
        // undeploy war
        deployer.undeploy(DEPLOYMENT_APP_ID);
        
        // check that the module and the classloader have been released
        assertClassloaderReleased();
        
    }
    
}
