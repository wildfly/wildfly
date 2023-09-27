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
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */

@Stateless
@SecurityDomain(value = "ejb3-tests", unauthenticatedPrincipal = "nobody")
public class SimpleSLSB implements Simple{
   public static final String SUCCESS = "SUCCESS";

   @RolesAllowed("MDBrole")
   public String testAuthorizedRole() {
      return SUCCESS;
   }

   @RolesAllowed("SomeOtherRole")
   public String testUnauthorizedRole() {
      return SUCCESS;
   }

   @PermitAll
   public String testPermitAll() {
      return SUCCESS;
   }

   @DenyAll
   public String testDenyAll() {
      return SUCCESS;
   }
}
