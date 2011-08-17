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
package org.jboss.as.test.spec.ejb3.security.runas;

import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import org.jboss.as.test.spec.ejb3.security.WhoAmI;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@LocalBean
@RolesAllowed("Role2")
@TransactionAttribute(SUPPORTS)
@SecurityDomain("ejb3-tests")
public class WhoAmIBean extends org.jboss.as.test.spec.ejb3.security.base.WhoAmIBean implements WhoAmI {

    // TODO - Do I really need to override methods and do they really need to be annotated individually.

    @Override
    @RolesAllowed("Role2")
    public Principal getCallerPrincipal() {
        return super.getCallerPrincipal();
    }

    @Override
    @RolesAllowed("Role2")
    public boolean doIHaveRole(String roleName) {
        return super.doIHaveRole(roleName);
    }

    @RolesAllowed("Role1")
    public void onlyRole1() {
        throw new AssertionError("Should not come here");
    }
}
