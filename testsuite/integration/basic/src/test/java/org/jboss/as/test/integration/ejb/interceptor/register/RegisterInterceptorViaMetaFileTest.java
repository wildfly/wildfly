/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;

/**
 * @author Jiri Bilek
 * jbilek@redhat.com on 12/03/18.
 * Test for WFLY-9844
 *
 * EJB invocation for Remote interface fails when Client Interceptor
 * registered via META-INF/services/org.jboss.ejb.client.EJBClientInterceptor
 */
@RunWith(Arquillian.class)
public class RegisterInterceptorViaMetaFileTest {
   private static final String ARCHIVE_NAME = "test-register-interceptor";
   public static final String clientInterceptorPrefix = "ClientInterceptor: ";

   @Deployment(name = "test-register-interceptor")
   public static Archive<?> deploy() {
      JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
      jar.addPackage(TestRemote.class.getPackage());
      jar.addPackage(TestSingleton.class.getPackage());
      jar.addPackage(TestSLSB.class.getPackage());
      jar.addAsManifestResource(
            RegisterInterceptorViaMetaFileTest.class.getPackage(),
            "org.jboss.ejb.client.EJBClientInterceptor",
            "services/org.jboss.ejb.client.EJBClientInterceptor");
      return jar;
   }

   @EJB
   TestSingleton testSingleton;

   @Test
   public void testInvokeSLSBthoughSingleton() throws Exception {
      String echo = "this it testing string";
      String result = testSingleton.test(echo);
      // ClientInterceptor append variable clientInterceptorPrefix on start of string so returned value is "clientInterceptorPrefix + echo"
      Assert.assertTrue("SLSB returned wrong value through singleton", result.equals(clientInterceptorPrefix + echo));
   }
}
