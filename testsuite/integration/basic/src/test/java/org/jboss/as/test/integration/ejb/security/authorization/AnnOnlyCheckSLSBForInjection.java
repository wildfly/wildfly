/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

/**
 * This is a no-interface session bean which is to be injected.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@Stateless
@SecurityDomain("other")
public class AnnOnlyCheckSLSBForInjection {

   public String defaultAccess(String message) {
      return message;
   }

   @RolesAllowed("Role1")
   public String roleBasedAccessOne(String message) {
      return message;
   }

   @RolesAllowed({"Role2", "Negative", "No-role"})
   public String roleBasedAccessMore(String message) {
      return message;
   }

   @PermitAll
   public String permitAll(String message) {
      return message;
   }

   @DenyAll
   public String denyAll(String message) {
      return message;
   }

   @RolesAllowed("**")
   public String starRoleAllowed(String message) {
      return message;
   }

}
