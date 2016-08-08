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
package org.jboss.as.test.clustering.cluster.classloader.leak;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.classloader.leak.AbstractClassloaderLeakTestBase;
import org.jboss.as.test.classloader.leak.ejb.StatefulRemote;
import org.jboss.as.test.classloader.leak.ejb.StatelessRemote;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.single.web.SimpleWebTestCase;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;

/**
 * Test for classloader leak following deployment, usage and undeployment
 * of distributed WAR application containing Servlet and JSP page and 
 * EAR application containg stateless, stateful and entity beans.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class ClassloaderLeakDistributedAppsTestCase extends AbstractClassloaderLeakTestBase {
    
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;
    
    private static final String DEPLOYMENT_WAR_ID = "deployment_war";    
    private static final String DEPLOYMENT_WAR_NAME = "ClassloaderLeakDistributedWarTestApp";    
    private static final String DEPLOYMENT_EAR_ID = "deployment_ear";    
    private static final String DEPLOYMENT_EAR_NAME = "ClassloaderLeakDistributedEarTestApp";    
    
    /*
     * Deployment containing the test case holding classloader reference.
     */
    @Deployment(name = DEPLOYMENT_DRIVER_ID, managed = false)
    @TargetsContainer(CONTAINER_1)    
    public static Archive<?> getDeployment() {                      
        WebArchive war = prepareDriverWar();
        war.addClass(ClassloaderLeakDistributedAppsTestCase.class);
        war.addClass(StatelessRemote.class);
        war.addClass(StatefulRemote.class);        
        return war;
    }        
    
    /*
     * WAR Deployment tested for classloader leak.
     *
     */
    @Deployment(name = DEPLOYMENT_WAR_ID, managed = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> getDeployment2() {
        WebArchive war = prepareTestAppWar(DEPLOYMENT_WAR_NAME);
        // set dependency to the test deployment
        war.setManifest(new StringAsset(
                "Manifest-Version: 1.0" + NL + 
                "Dependencies: deployment." + DEPLOYMENT_DRIVER_NAME + ", org.jboss.modules" + NL)
                );
        // mark the app as distributable
        war.setWebXML(SimpleWebTestCase.class.getPackage(), "web.xml");
        return war;
    } 
    
    /*
     * EAR Deployment tested for classloader leak.
     */
    @Deployment(name = DEPLOYMENT_EAR_ID, managed = false)
    public static Archive<?> getDeployment3() {
        WebArchive war = prepareTestAppWar(DEPLOYMENT_EAR_NAME);
        // mark the app as distributable
        war.setWebXML(SimpleWebTestCase.class.getPackage(), "web.xml");
        EnterpriseArchive ear = prepareTestAppEar(DEPLOYMENT_EAR_NAME, war, true);
        // set dependency to the test deployment
        ear.setManifest(new StringAsset(
                "Manifest-Version: 1.0" + NL + 
                 "Dependencies: deployment." + DEPLOYMENT_DRIVER_NAME + ", org.jboss.modules" + NL)
                );
        
        return ear;
    } 
    
    @Test
    @InSequence(-1)
    @RunAsClient
    public void testStartContainers() {
        NodeUtil.start(controller, deployer, CONTAINER_1, DEPLOYMENT_DRIVER_ID);
        NodeUtil.start(controller, CONTAINER_2);
    }

    @Test
    @InSequence(1)
    @RunAsClient
    public void testStopContainers() {
        NodeUtil.stop(controller, deployer, CONTAINER_1, DEPLOYMENT_DRIVER_ID);
        NodeUtil.stop(controller, CONTAINER_2);
    }    
    
    @Test
    @OperateOnDeployment(DEPLOYMENT_DRIVER_ID)
    public void testWarModuleRelease() throws Exception {
        
        assertClassloaderNotRegistered();
        
        // deploy war
        deployer.deploy(DEPLOYMENT_WAR_ID);
        
        // check that the application classloader is registered
        assertClassloaderRegistered();
        
        // test war
        testWar(webURI + "/" + DEPLOYMENT_WAR_NAME);
        
        // undeploy war
        deployer.undeploy(DEPLOYMENT_WAR_ID);
        
        // check that the module and the classloader have been released
        assertClassloaderReleased();  
        
        clearClassloaderRefs();
        
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DRIVER_ID)
    public void testEarModuleRelease() throws Exception {
        
        assertClassloaderNotRegistered();
        
        // deploy ear
        deployer.deploy(DEPLOYMENT_EAR_ID);
        
        // check that the application classloader is registered
        assertClassloaderRegistered();
        
        // test ear
        testWar(webURI + "/" + DEPLOYMENT_EAR_NAME);
        
        // access ejbs
        testEjb(DEPLOYMENT_EAR_NAME, true);
        
        // undeploy ear
        deployer.undeploy(DEPLOYMENT_EAR_ID);
        
        // check that the module and the classloader have been released
        assertClassloaderReleased();                
        
        clearClassloaderRefs();
        
    }
    
}
