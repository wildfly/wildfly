/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.interceptor.wildcard;

import javax.ejb.EJB;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(SetupModuleServerSetupTask.class)
public class WildcardInterceptorTestCase {
    
    
    @ArquillianResource 
    Deployer deployer;
    @EJB
    InterceptorNotification interceptor;
    
    @ArquillianResource
    Context context;

    @Deployment(name = Constants.TEST_DEPLOYMENT_NAME, managed = true, testable = true)
    public static Archive<?> getTestDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,Constants.TEST_DU);
        jar.addClasses(WildcardInterceptorTestCase.class, InvocationCounter.class, Constants.class,SetupModuleServerSetupTask.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }

    @Deployment(name = Constants.TEST_EJB_DEPLOYMENT_NAME, managed = false, testable = false)
    public static Archive<?> getTestSubjectDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,Constants.TEST_EJB_DU);
        jar.addClasses(DummyInterceptor.class,SimpleSLSBean.class);
        jar.addAsManifestResource(getEjbJar(), "ejb-jar.xml");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }
    
    
    /**
     * @return
     */
    private static StringAsset getEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "   <assembly-descriptor>\n" +
                "       <interceptor-binding>\n" +
                "           <ejb-name>*</ejb-name>\n" +
                "           <interceptor-class>org.jboss.as.test.integration.ejb.interceptor.wildcard.DummyInterceptor</interceptor-class>\n" +
                "       </interceptor-binding>\n" +
                "   </assembly-descriptor>\n" +
                "   \n" +
                "</ejb-jar>");
    }

    @Test
    public void testInterceptorCount() throws Exception {
        this.deployer.deploy(Constants.TEST_EJB_DEPLOYMENT_NAME);
        final InterceptedSLSBeanFace slsBean = ((InterceptedSLSBeanFace)context.lookup(Constants.INTERCEPTED_BEAN_JNDI_NAME));
        slsBean.hello();
        Assert.assertEquals(1, interceptor.getCount());
        this.deployer.undeploy(Constants.TEST_EJB_DEPLOYMENT_NAME);
    }
}
