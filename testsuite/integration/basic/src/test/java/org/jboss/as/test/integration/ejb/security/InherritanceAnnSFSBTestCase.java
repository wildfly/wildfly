/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.ejb.security.authorization.InherritanceAnnOnlyCheckSFSB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * This test case check whether basic EJB authorization works from EJB client to inherrited stateless remote EJB.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@Tag("CommonCriteria")
public class InherritanceAnnSFSBTestCase extends AnnSBTest{

   private static final Logger log = Logger.getLogger(testClass());
   private static final String MODULE = "inherritanceAnnOnlySFSB";

   private static Class testClass() {
      return InherritanceAnnSFSBTestCase.class;
   }

   private static Class beanClass() {
      return InherritanceAnnOnlyCheckSFSB.class;
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
