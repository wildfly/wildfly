/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.authorization.InjectionSLSBtoSLSB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;



/**
 * This test case check whether basic EJB authorization works from EJB client to injected stateless remote EJB.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class InjectionAnnSLSBtoSLSBTestCase extends AnnSBTest {

   private static final Logger log = Logger.getLogger(testClass());
   private static final String MODULE = "injectionAnnOnlySLSBtoSLSB";

   private static Class testClass(){
      return InjectionAnnSLSBtoSLSBTestCase.class;
   }

   private static Class beanClass(){
      return InjectionSLSBtoSLSB.class;
   }

   @Deployment(name = MODULE + ".jar", order = 1, testable = false)
   public static Archive<JavaArchive> deployment() {
      return testAppDeployment(Logger.getLogger(testClass()), MODULE, beanClass());
   }

   @Test
   public void testSingleMethodAnnotationsNoUser() throws Exception {
      testSingleMethodAnnotationsNoUserTemplate(MODULE, log, beanClass());
   }

   @Test
   public void testSingleMethodAnnotationsUser1() throws Exception {
      testSingleMethodAnnotationsUser1Template(MODULE, log, beanClass());
   }

   @Test
   public void testSingleMethodAnnotationsUser2() throws Exception {
      testSingleMethodAnnotationsUser2Template(MODULE, log, beanClass());
   }
}