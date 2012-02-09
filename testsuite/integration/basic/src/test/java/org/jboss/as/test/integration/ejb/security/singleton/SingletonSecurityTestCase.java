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

package org.jboss.as.test.integration.ejb.security.singleton;

import java.util.logging.Logger;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.security.SecurityTest;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations on a secured singleton bean work as expected.
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 * 
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class SingletonSecurityTestCase extends SecurityTest {
    private static final Logger log = Logger.getLogger(SingletonSecurityTestCase.class.getName()); 
    
    @Deployment
    public static Archive<?> deploy() {
    	try {
            // create required security domains
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-security.jar");
        jar.addPackage(SingletonSecurityTestCase.class.getPackage());
        jar.addClass(SecurityTest.class);
        jar.addAsResource(SingletonSecurityTestCase.class.getPackage(), "users.properties", "users.properties");
        jar.addAsResource(SingletonSecurityTestCase.class.getPackage(), "roles.properties", "roles.properties");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr\n"),"MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    /**
     * Test a method invocation on a singleton bean with the correct expected role.
     * 
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        final SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("user1", "pass1");
        try {
            // login
            securityClient.login();
            // expects role1, so should succeed
            securedSingleton.allowedForRole1();
        } finally {
            securityClient.logout();
        }

    }

    /**
     * Test a method invocation on a singleton bean with an incorrect role.
     * 
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithInCorrectRole() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        final SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("user2", "pass2");
        try {
            // login
            securityClient.login();
            try {
                // expects role1, so should fail
                securedSingleton.allowedForRole1();
                Assert.fail("Call to secured method, with incorrect role, was expected to fail");
            } catch (EJBAccessException ejbae) {
                // expected
            }
        } finally {
            securityClient.logout();
        }

    }

    /**
     * Test a method invocation on a singleton bean without logging in.
     * 
     * @throws Exception
     */
    @Test
    public void testInvocationOnSecuredMethodWithoutLogin() throws Exception {
        final SingletonSecurity securedSingleton = InitialContext.doLookup("java:module/" + SecuredSingletonBean.class.getSimpleName());
        try {
            securedSingleton.allowedForRole1();
            Assert.fail("Call to secured method was expected to fail");
        } catch (EJBAccessException ejbae) {
            // expected
        }

    }
}
