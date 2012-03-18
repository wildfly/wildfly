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
package org.jboss.as.test.integration.jsf.jta.phase;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jsf.jta.JTATestsBase;
import org.jboss.as.test.integration.jsf.jta.SimpleLogin;
import org.jboss.as.test.integration.jsf.jta.customui.CustomUITestCase;
import org.jboss.jsfunit.api.InitialPage;
import org.jboss.jsfunit.jsfsession.JSFClientSession;
import org.jboss.jsfunit.jsfsession.JSFServerSession;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Simple class to test if phase listener has access to user transaction.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
public class PhaseListenerTestCase extends JTATestsBase{


    private static final String DEPLOYMENT_PHASE_CONTEXT = "jsf-jta-phase-listener";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_PHASE_CONTEXT + ".war";
    // ----------------- DEPLOYMENTS ------------

    @Deployment
    @OverProtocol("Servlet 3.0")
    public static Archive<WebArchive> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addPackage(PhaseListenerTestCase.class.getPackage())
                .addClasses(SimpleLogin.class, JTATestsBase.class)
                .addAsWebInfResources(PhaseListenerTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(PhaseListenerTestCase.class.getPackage(), "faces-config.xml", "faces-config.xml")
                .addAsWebResource(PhaseListenerTestCase.class.getPackage(), "index.xhtml", "index.xhtml");
    }

    @Test
    @InitialPage("/index.jsf")
    public void testPhaseListener(JSFServerSession server, JSFClientSession client) throws Exception {
        // NOTE: if I fail, check server log, JSFUnit masks real cause cause it seems to be last in chain
        //      , so if assertFails in PhaseListener class, it will show '500' return status saying
        //      'cant inject parameters' - it happens even when JSFSession is created by hand.

        Assert.assertNotNull(server);
        Assert.assertNotNull(client);
        Assert.assertEquals("Wrong view ID!!!", "/index.xhtml", server.getCurrentViewID());


    }
}