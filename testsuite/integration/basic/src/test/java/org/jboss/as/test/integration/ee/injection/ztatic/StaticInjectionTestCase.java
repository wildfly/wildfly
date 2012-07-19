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

package org.jboss.as.test.integration.ee.injection.ztatic;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the Resource injection as specified by Java EE spec works as expected
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StaticInjectionTestCase {

    private static final Logger logger = Logger.getLogger(StaticInjectionTestCase.class.getName());

    private static final String FIELD_DEPLOYMENT_NAME = "static-field-injection-test-du";
    private static final String METHOD_DEPLOYMENT_NAME = "static-method-injection-test-du";
    
    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = FIELD_DEPLOYMENT_NAME, managed = false)
    public static WebArchive createFieldTestDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, FIELD_DEPLOYMENT_NAME+".war");
        //war.addPackage(TestEJB.class.getPackage());
        war.addClasses(FieldTestEJB.class,StaticInjectionTestCase.class);
        war.addAsWebInfResource(StaticInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }
    
    @Deployment(name = METHOD_DEPLOYMENT_NAME, managed = false)
    public static WebArchive createMethodTestDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, METHOD_DEPLOYMENT_NAME+".war");
        war.addClasses(MethodTestEJB.class,StaticInjectionTestCase.class);
        war.addAsWebInfResource(StaticInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    /**
     * 
     */
    @Test
    public void testStaticFieldInjection() {
        try {
           deployer.deploy(FIELD_DEPLOYMENT_NAME);
           // deploy should have failed
           deployer.undeploy(FIELD_DEPLOYMENT_NAME);
           fail();
        }
        catch (Exception e) {
            logger.info(e);
        }
    }
    
    /**
     * 
     */
    @Test
    public void testStaticMethodInjection() {
        try {
           deployer.deploy(METHOD_DEPLOYMENT_NAME);
           // deploy should have failed
           deployer.undeploy(METHOD_DEPLOYMENT_NAME);
           fail();
        }
        catch (Exception e) {
            logger.info(e);
        }
    }
}
