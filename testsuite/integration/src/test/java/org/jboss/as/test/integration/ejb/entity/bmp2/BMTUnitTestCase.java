/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.bmp2;

import javax.naming.InitialContext;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;

import org.junit.Test;

/**
 * Sample client for the jboss container.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */

@RunWith(Arquillian.class)
public class BMTUnitTestCase
{
   private static final Logger log = Logger.getLogger(BMTUnitTestCase.class);

   static boolean deployed = false;
   static int test = 0;

   @Deployment
   public static Archive<?> deploy() {
       JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bmt2-test.jar");
       jar.addPackage(BMTUnitTestCase.class.getPackage());
       jar.addAsManifestResource("ejb/entity/bmp2/ejb-jar.xml", "ejb-jar.xml");
       
       log.error(jar.toString(Formatters.VERBOSE));
       
       return jar;
   }
   
   private InitialContext getInitialContext() throws Exception {
       return new InitialContext();
   }

   @Test
   public void testStateless() throws Exception
   {
      TesterRemote tester = (TesterRemote)getInitialContext().lookup("java:module/TesterBean");
      tester.testStatelessWithoutTx();
      tester.testStatelessWithTx();
   }

   @Test
   public void testStateful() throws Exception
   {
      TesterRemote tester = (TesterRemote)getInitialContext().lookup("java:module/TesterBean");
      tester.testStatefulWithoutTx();
      tester.testStatefulWithTx();
   }
   
   @Test
   public void testDeploymentDescriptorStateless() throws Exception
   {
      DeploymentDescriptorTesterRemote tester = (DeploymentDescriptorTesterRemote)getInitialContext().lookup("java:module/DeploymentDescriptorTester");
      tester.testStatelessWithoutTx();
      tester.testStatelessWithTx();
   }

   @Test
   public void testDeploymentDescriptorStateful() throws Exception
   {
      DeploymentDescriptorTesterRemote tester = (DeploymentDescriptorTesterRemote)getInitialContext().lookup("java:module/DeploymentDescriptorTester");
      tester.testStatefulWithoutTx();
      tester.testStatefulWithTx();
   }
}
