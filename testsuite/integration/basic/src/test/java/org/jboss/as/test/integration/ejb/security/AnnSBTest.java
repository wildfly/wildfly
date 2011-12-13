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
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSFSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSLSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.ParentAnnOnlyCheck;
import org.jboss.as.test.integration.ejb.security.authorization.SimpleAuthorizationRemote;
import org.jboss.as.test.integration.ejb.security.authorization.SingleMethodsAnnOnlyCheckSFSB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.security.auth.login.LoginContext;

/**
 * This is a common parent for test cases to check whether basic EJB authorization works from an EJB client to a remote EJB.
 *
 * @author <a href="mailto:jan.lanik@redhat.com">Jan Lanik</a>
 */
public abstract class AnnSBTest extends SecurityTest {

   public static Archive<JavaArchive> testAppDeployment(final Logger LOG, final String MODULE, final Class SB_TO_TEST ) {

      // FIXME: change when there will be an option to deploy/call something before the first deployment
      try {
         // create required security domain
         createSecurityDomain();
      } catch (Exception e) {
         LOG.warn("Problems during creation of security domain", e);
      }
      LOG.info("Security domain: ejb3-tests created");


      // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
      final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar")
         .addClass(SB_TO_TEST)
         .addClass(SimpleAuthorizationRemote.class)
         .addClass(ParentAnnOnlyCheck.class)
         .addClass(AnnOnlyCheckSLSBForInjection.class)
         .addClass(AnnOnlyCheckSFSBForInjection.class)
            //.addClass(Util.class)
            //.addClass(SecurityTest.class)
         .addAsResource("ejb3/security/users.properties", "users.properties")
         .addAsResource("ejb3/security/roles.properties", "roles.properties")
         .addAsManifestResource("ejb3/security/EMPTY_MANIFEST.MF", "MANIFEST.MF");
      LOG.info(jar.toString(true));
      return jar;
   }
   

   /**
    * Test objective:
    *   Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
    *   works on method level without user logged in as described in EJB 3.1 spec.
    *   The target session bean is given as parameter
    * Expected results:
    *   Test has to finish without any exception or error.
    *
    * @throws Exception
    */
   public void testSingleMethodAnnotationsNoUserTemplate(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {

      String myContext = Util.createRemoteEjbJndiContext(
         "",
         MODULE,
         "",
         SB_CLASS.getSimpleName(),
         SimpleAuthorizationRemote.class.getName(),
         isBeanClassStatefull(SB_CLASS));

      log.info("JNDI name=" + myContext);

      final Context ctx = Util.createNamingContext();
      final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote)
         ctx.lookup(myContext);

      try {
         String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
         Assert.assertEquals(echoValue, "alohomora");
      } catch (EJBAccessException e) {
         Assert.fail("Exception not expected");
      }


      try {
         String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
         Assert.fail("Method cannot be successfully called without logged in user");
      } catch (Exception e) {
         // expected
         Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
      }

      try {
         String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
         Assert.fail("Method cannot be successfully called without logged in user");
      } catch (Exception e) {
         // expected
         Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
      }

      try {
         String echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
         Assert.assertEquals(echoValue, "alohomora");
      } catch (Exception e) {
         Assert.fail("@PermitAll annotation must allow all users and no users to call the method");
      }

      try {
         String echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
         Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
      } catch (Exception e) {
         // expected
         Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
      }


   }

   /**
    * Test objective:
    *   Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
    *   works on method level with user1 logged in as described in EJB 3.1 spec.
    *   user1 has "Users,Role1" roles.
    *   The target session bean is given as parameter.
    * Expected results:
    *   Test has to finish without any exception or error.
    *
    * TODO: remove @Ignore after the JIRA is fixed
    *
    * @throws Exception
    */
   @Ignore("AS7-2942")
   public void testSingleMethodAnnotationsUser1Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {

      LoginContext lc = Util.getCLMLoginContext("user1", "password1");
      lc.login();

      try {

         String myContext = Util.createRemoteEjbJndiContext(
            "",
            MODULE,
            "",
            SB_CLASS.getSimpleName(),
            SimpleAuthorizationRemote.class.getName(),
            isBeanClassStatefull(SB_CLASS));
         log.info("JNDI name=" + myContext);

         final Context ctx = Util.createNamingContext();
         final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote)
            ctx.lookup(myContext);

         try {
            String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (EJBAccessException e) {
            Assert.fail("EJBAccessException not expected");
         }


         try {
            String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (EJBAccessException e) {
            Assert.fail("EJBAccessException not expected");
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
            Assert.fail("Method cannot be successfully called with logged in principal:" + lc.getSubject());
         } catch (Exception e) {
            // expected
            Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (Exception e) {
            Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal:" + lc.getSubject());
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
            Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
         } catch (Exception e) {
            // expected
            Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
         }

      } finally {
         lc.logout();
      }


   }

   /**
    * Test objective:
    *   Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
    *   works on method level with user1 logged in as described in EJB 3.1 spec.
    *   user2 has "Users,Role2" roles.
    *   The target session bean is given as parameter.
    * Expected results:
    *   Test has to finish without any exception or error.
    *
    * TODO: remove @Ignore after the JIRA is fixed
    *
    * @throws Exception
    */
   @Ignore("AS7-2942")
   public void testSingleMethodAnnotationsUser2Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
      LoginContext lc = Util.getCLMLoginContext("user2", "password2");
      lc.login();

      try {

         String myContext = Util.createRemoteEjbJndiContext(
            "",
            MODULE,
            "",
            SB_CLASS.getSimpleName(),
            SimpleAuthorizationRemote.class.getName(),
            isBeanClassStatefull(SB_CLASS));
         log.info("JNDI name=" + myContext);

         final Context ctx = Util.createNamingContext();
         final SimpleAuthorizationRemote singleMethodsAnnOnlyBean = (SimpleAuthorizationRemote)
            ctx.lookup(myContext);

         try {
            String echoValue = singleMethodsAnnOnlyBean.defaultAccess("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (EJBAccessException e) {
            Assert.fail("EJBAccessException not expected");
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessOne("alohomora");
            Assert.fail("Method cannot be successfully called with logged in user2");
         } catch (Exception e) {
            // expected
            Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
         }



         try {
            String echoValue = singleMethodsAnnOnlyBean.roleBasedAccessMore("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (EJBAccessException e) {
            Assert.fail("EJBAccessException not expected");
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.permitAll("alohomora");
            Assert.assertEquals(echoValue, "alohomora");
         } catch (Exception e) {
            Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal:" + lc.getSubject());
         }

         try {
            String echoValue = singleMethodsAnnOnlyBean.denyAll("alohomora");
            Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
         } catch (Exception e) {
            // expected
            Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
         }

      } finally {
         lc.logout();
      }

   }

   protected static boolean isBeanClassStatefull(Class bean){
      if(bean.getName().contains("toSLSB")){
         return false;
      } else if (bean.getName().contains("toSFSB")){
         return true;
      } else if (bean.getName().contains("SFSB")){
         return true;
      } else if (bean.getName().contains("SLSB")){
         return false;
      } else {
         throw new AssertionError("Session bean class has a wrong name!:  " + bean.getCanonicalName());
      }
   }

}

