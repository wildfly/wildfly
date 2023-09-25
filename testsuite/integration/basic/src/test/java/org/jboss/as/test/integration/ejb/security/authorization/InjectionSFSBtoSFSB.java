/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@Stateful
@SecurityDomain("other")
public class InjectionSFSBtoSFSB implements SimpleAuthorizationRemote {

   @EJB AnnOnlyCheckSFSBForInjection injected;

   public String defaultAccess(String message) {
      return injected.defaultAccess(message);
   }

   public String roleBasedAccessOne(String message) {
      return injected.roleBasedAccessOne(message);
   }

   public String roleBasedAccessMore(String message) {
      return injected.roleBasedAccessMore(message);
   }

   public String permitAll(String message) {
      return injected.permitAll(message);
   }

   public String denyAll(String message) {
      return injected.denyAll(message);
   }

   public String starRoleAllowed(String message) {
      return injected.starRoleAllowed(message);
   }
}
