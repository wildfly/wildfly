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

package org.jboss.as.test.integration.ejb.security;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Local({Restriction.class, FullAccess.class})
@LocalBean
public class BeanWithoutExplicitSecurityDomain implements Restriction, FullAccess {

    @Resource
    private SessionContext sessionContext;

    @Override
    @DenyAll
    public void restrictedMethod() {
        throw new RuntimeException("No one was expected to be able to call this method");
    }

    @Override
    @PermitAll
    public void doAnything() {
    }


    @RolesAllowed("Role1")
    public void allowOnlyRoleOneToAccess() {
        if (!this.sessionContext.isCallerInRole("Role1")) {
            throw new RuntimeException("Only user(s) in role1 were expected to have access to this method");
        }
    }

    @RolesAllowed("Role2")
    public void allowOnlyRoleTwoToAccess() {
        if (!this.sessionContext.isCallerInRole("Role2")) {
            throw new RuntimeException("Only user(s) in role2 were expected to have access to this method");
        }
    }
}
