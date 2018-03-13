/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
   @ArquillianResource
   Deployer deployer;

   @Deployment(name = "test-register-interceptor", testable = false, managed = false)
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

   @Test
   public void testInvalidTransactionAttributeWarnLogged() {
      PrintStream oldOut = System.out;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
         System.setOut(new PrintStream(baos));
         deployer.deploy(ARCHIVE_NAME);
         try {
            System.setOut(oldOut);
            String output = new String(baos.toByteArray());
            Assert.assertFalse(output, output.contains("EJBCLIENT000079"));
            Assert.assertFalse(output, output.contains("ERROR"));
         } finally {
            deployer.undeploy(ARCHIVE_NAME);
         }
      } finally {
         System.setOut(oldOut);
      }
   }
}
