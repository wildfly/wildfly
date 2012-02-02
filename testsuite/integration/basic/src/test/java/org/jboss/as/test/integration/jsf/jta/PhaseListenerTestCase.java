/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jsf.jta;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jsf.jta.login.SimpleLogin;
import org.jboss.as.test.integration.jsf.jta.phase.JTAPhaseListener;
import org.jboss.jsfunit.api.InitialPage;
import org.jboss.jsfunit.jsfsession.JSFClientSession;
import org.jboss.jsfunit.jsfsession.JSFServerSession;
import org.jboss.jsfunit.jsfsession.JSFSession;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * 
 * Simple class to test if phase listener has access to user transaction.
 * @author baranowb
 * 
 */
@RunWith(Arquillian.class)
public class PhaseListenerTestCase extends JTATestsBase {


    private static final Logger log = Logger.getLogger(PhaseListenerTestCase.class.getName());
    private static final String DEPLOYMENT_PHASE_CONTEXT = "jsf-jta-phase-listener";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_PHASE_CONTEXT+".war";
    private static final String RESOURCES_LOCATION = "org/jboss/as/test/integration/jsf/jta/phase";
    // ----------------- DEPLOYMENTS ------------

    //@ArquillianResource
    //URL deploymentURL;
    
    
    @Deployment
    public static Archive<WebArchive> createDeployment() {

        // add test classes
        Class[] classes = new Class[]{SimpleLogin.class,ManagedBeanTestCase.class};
        String[] resources = new String[]{"index.xhtml"};
        String[] webInfResources = new String[]{"web.xml","faces-config.xml","jboss-deployment-structure.xml"};
        
        final WebArchive archive = createArchive(DEPLOYMENT_NAME, classes, null, RESOURCES_LOCATION, resources, webInfResources);
        log.info(archive.toString(true));
        
        return archive;
    }

    @Test
    @InitialPage("/index.jsf")
    public void testPhaseListener(JSFServerSession server, JSFClientSession client) throws Exception{
        // NOTE: if I fail, check server log, JSFUnit masks real cause cause it seems to be last in chain
        //      , so if assertFails in PhaseListener class, it will show '500' return status saying 
        //      'cant inject parameters' - it happens even when JSFSession is created by hand.
       
    	Assert.assertNotNull(server);
    	Assert.assertNotNull(client);
    	Assert.assertEquals("Wrong view ID!!!", "/index.xhtml",server.getCurrentViewID());
    	
    	
    }
   
}