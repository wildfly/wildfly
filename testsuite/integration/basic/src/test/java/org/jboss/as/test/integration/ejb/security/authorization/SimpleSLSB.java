/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;

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
