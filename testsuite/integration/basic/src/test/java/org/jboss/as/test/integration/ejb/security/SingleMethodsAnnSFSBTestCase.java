/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.ejb.security.authorization.SingleMethodsAnnOnlyCheckSFSB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This test case check whether basic EJB authorization works from EJB client to remote stateful EJB.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@Tag("CommonCriteria")
public class SingleMethodsAnnSFSBTestCase extends AnnSBTest {

    private static final Logger log = Logger.getLogger(testClass());
    private static final String MODULE = "singleMethodsAnnOnlySFSB";

   private static Class testClass() {
      return SingleMethodsAnnSFSBTestCase.class;
   }

   private static Class beanClass() {
      return SingleMethodsAnnOnlyCheckSFSB.class;
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
