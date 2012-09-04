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

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * <p>
 * Stateless session bean implementation used in the EJB3 security tests.
 * </p>
 * 
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Stateless
@Remote(SimpleSession.class)
@DeclareRoles({ "RegularUser", "Administrator" })
@RolesAllowed({ "RegularUser", "Administrator" })
public class SimpleStatelessSessionBean implements SimpleSession {

    @Resource
    private SessionContext context;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.test.security.ejb3.SimpleSession#invokeRegularMethod()
     */
    public Principal invokeRegularMethod() {
        // this method allows the same roles as the class.
        return this.context.getCallerPrincipal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.test.security.ejb3.SimpleSession#invokerAdministrativeMethod()
     */
    @RolesAllowed({ "Administrator" })
    public Principal invokeAdministrativeMethod() {
        // this method overrides the roles defined by the class to grant access to admnistrators only.
        return this.context.getCallerPrincipal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.test.security.ejb3.SimpleSession#invokeUnprotectedMethod()
     */
    @PermitAll
    public Principal invokeUnprotectedMethod() {
        // this method overrides the roles defined by the class to grant access to all roles.
        return this.context.getCallerPrincipal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.test.security.ejb3.SimpleSession#invokeUnavailableMethod()
     */
    @DenyAll
    public Principal invokeUnavailableMethod() {
        // this method should never be called - it overrides the class roles to deny access to all roles.
        return this.context.getCallerPrincipal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.test.security.ejb3.SimpleSession#checkDeclaredRoles(java.lang.String[])
     */
    @Override
    public boolean checkDeclaredRoles(String... rolesToCheck) {

        if (rolesToCheck.length < 1)
            throw new IllegalArgumentException("Empty roles. You have to supply at least one role.");

        boolean result = true;

        for (String roleName : rolesToCheck) {

            boolean negative = roleName.substring(0, 1).equals("!");
            if (negative)
                roleName = roleName.substring(1);

            result = result && (!negative == this.context.isCallerInRole(roleName));
        }

        return result;

    }

}
