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

package org.jboss.as.test.integration.ejb.security.asynchronous;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (asynchronous) to AS7 [JIRA JBQA-5483].
 * 
 * Test if asynchronous calls and security check works. 
 */
@RunWith(Arquillian.class)
public class AsynchronousSecurityTestCase {
    private static final Logger log = Logger.getLogger(AsynchronousSecurityTestCase.class);
    private static final String ARCHIVE_NAME = "AsyncSecurityTestCase";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(AsynchronousSecurityTestCase.class.getPackage());
        jar.addAsResource(AsynchronousSecurityTestCase.class.getPackage(), "roles.properties", "roles.properties");
        jar.addAsResource(AsynchronousSecurityTestCase.class.getPackage(), "users.properties", "users.properties");
        log.info(jar.toString(true));
        return jar;
    }
    
    protected <Q, T> Q lookupInterface(Class<T> bean, Class<Q> intf) throws NamingException {
        log.info("initctx: " + iniCtx);
        return intf.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + bean.getSimpleName() + "!"
                + intf.getName()));
    }
    
    @Test
    public void testAsynchSecurityMethod() throws Exception {
        SecuredStatelessRemote securedBean = lookupInterface(SecuredStatelessBean.class, SecuredStatelessRemote.class);

        SecurityClient client = SecurityClientFactory.getSecurityClient();
        boolean result = false;
        Future<Boolean> future;
        
        // Test 1
        SecuredStatelessBean.reset();
        client.logout();
        client.setSimple("somebody", "password");
        client.login();
        
        future = securedBean.method();
        SecuredStatelessBean.startLatch.countDown();
        result = future.get();
        Assert.assertTrue(result);
        
        // Test 2
        SecuredStatelessBean.reset();
        future = null;
        result = false;
        client.logout();
        client.setSimple("rolefail", "password");
        client.login();
        
        try {
            future = securedBean.method();
            SecuredStatelessBean.startLatch.countDown();
            result = future.get();
        } catch (ExecutionException ee) {
            if(!(ee.getCause() instanceof EJBAccessException)) {
                Assert.fail("Exception cause was not EJBAccessException and was " + ee);
            }
        } catch (EJBAccessException ejbe) {
            // it's ok too
        }
        Assert.assertFalse(result);

        // Test 3
        SecuredStatelessBean.reset();
        future = null;
        result = false;
        client.logout();
        client.setSimple("nosuchuser", "password");
        client.login();
        
        try {
            future = securedBean.method();
            SecuredStatelessBean.startLatch.countDown();
            result = future.get();
        } catch (ExecutionException ee) {
            if(!(ee.getCause() instanceof EJBAccessException)) {
                Assert.fail("Exception cause was not EJBAccessException and was " + ee);
            }
        } catch (EJBAccessException ejbe) {
            // it's ok too
        }
        Assert.assertFalse(result);
    }

    
    @Test
    public void testAsyncSecurityPermition() throws Exception {
        SecuredStatelessBean.reset();
        SecuredStatelessRemote securedBean = lookupInterface(SecuredStatelessBean.class, SecuredStatelessRemote.class);

        SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("rolefail", "password");
        client.login();
        
        // Test 1
        Future<Boolean> future = securedBean.uncheckedMethod();
        SecuredStatelessBean.startLatch.countDown();
        boolean result = future.get();
        Assert.assertTrue(result);

        // Test 2
        future = null;
        result = false;
        SecuredStatelessBean.reset();
        try {
            future = securedBean.excludedMethod();
            SecuredStatelessBean.startLatch.countDown();
            result = future.get();
        } catch (ExecutionException ee) {
            if(!(ee.getCause() instanceof EJBAccessException)) {
                Assert.fail("Exception cause was not EJBAccessException and was " + ee);
            }
        } catch (EJBAccessException ejbe) {
            // it's ok too
        }
        Assert.assertFalse(result);
    }
}
