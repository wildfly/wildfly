/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.security.Principal;

/**
 * <p>
 * This is the remote interface of session beans used in the EJB3 security tests.
 * </p>
 * 
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface SimpleSession {

    /**
     * <p>
     * This is a method available for regular users and administrators. Implementations must annotate either the class or this
     * method with {@code @RolesAllowed( "RegularUser", "Administrator"})} to enforce that only these roles should be granted
     * access to this method.
     * </p>
     * 
     * @return the caller's {@code Principal}.
     */
    public Principal invokeRegularMethod();

    /**
     * <p>
     * This is a method available for administrators only. Implementations must annotate either the class or this method with
     * {@code @RolesAllowed( "Administrator"})} to enforce that only administrators should be granted access to this method.
     * </p>
     * 
     * @return the caller's {@code Principal}.
     */
    public Principal invokeAdministrativeMethod();

    /**
     * <p>
     * This is a method available for all authenticated users, regardless or role. Implementations must annotate this method
     * with {@code @PermitAll} to specify that all security roles should be granted access.
     * </p>
     * 
     * @return the caller's {@code Principal}.
     */
    public Principal invokeUnprotectedMethod();

    /**
     * <p>
     * This is a method that is unavailable for everybody. Implementations must annotate this method with {@code @DenyAll} to
     * specify that access should be restricted for everybody.
     * </p>
     * 
     * @return the caller's {@code Principal}.
     */
    public Principal invokeUnavailableMethod();

    /**
     * <p>
     * This method checks the roles specified as parameters to verify {@code @DeclareRoles} annotation.
     * </p>
     * 
     * @return result of the check
     */
    public boolean checkDeclaredRoles(String... rolesToCheck);

}