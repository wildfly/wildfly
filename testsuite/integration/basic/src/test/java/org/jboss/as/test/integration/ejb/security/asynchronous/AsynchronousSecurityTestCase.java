/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.AnnotationAuthorizationTestCase;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (asynchronous) to AS7 [JIRA JBQA-5483].
 * 
 * Test if asynchronous calls and security check works. 
 */
@RunWith(Arquillian.class)
@ServerSetup({AsynchronousSecurityTestCase.AsynchronousSecurityTestCaseSetup.class})
@Category(CommonCriteria.class)
public class AsynchronousSecurityTestCase {
    private static final Logger log = Logger.getLogger(AsynchronousSecurityTestCase.class);
    private static final String ARCHIVE_NAME = "AsyncSecurityTestCase";

    static class AsynchronousSecurityTestCaseSetup extends EjbSecurityDomainSetup {
        @Override
        protected String getSecurityDomainName() {
            return "async-security-test";
        }
    }
    
    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {       
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(AsynchronousSecurityTestCase.class.getPackage());
        jar.addClass(AnnotationAuthorizationTestCase.class);
        jar.addClasses(EjbSecurityDomainSetup.class, AbstractSecurityDomainSetup.class, ServerSetupTask.class, Util.class);
        jar.addAsResource(AsynchronousSecurityTestCase.class.getPackage(), "roles.properties", "roles.properties");
        jar.addAsResource(AsynchronousSecurityTestCase.class.getPackage(), "users.properties", "users.properties");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr\n"),"MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
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

        boolean result = false;
        Future<Boolean> future;
        
        // Test 1
        SecuredStatelessBean.reset();
        
        LoginContext lc = Util.getCLMLoginContext("somebody", "password");
        lc.login();
        
        try {
            future = securedBean.method();
            SecuredStatelessBean.startLatch.countDown();
            result = future.get();
        } finally {
            lc.logout();
        }
        Assert.assertTrue(result);
        
        // Test 2
        SecuredStatelessBean.reset();
        future = null;
        result = false;
        lc = Util.getCLMLoginContext("rolefail", "password");
        lc.login();
        
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
        } finally {
            lc.logout();
        }
        Assert.assertFalse(result);

        // Test 3
        SecuredStatelessBean.reset();
        future = null;
        result = false;
        lc = Util.getCLMLoginContext("nosuchuser", "password");
        lc.login();
        
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
        } finally {
            lc.logout();
        }
        Assert.assertFalse(result);
    }

    
    @Test
    public void testAsyncSecurityPermition() throws Exception {
        SecuredStatelessBean.reset();
        SecuredStatelessRemote securedBean = lookupInterface(SecuredStatelessBean.class, SecuredStatelessRemote.class);

        LoginContext lc = Util.getCLMLoginContext("rolefail", "password");
        lc.login();
        
        // Test 1
        try {
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
        } finally {
            lc.logout();
        }
    }
}
