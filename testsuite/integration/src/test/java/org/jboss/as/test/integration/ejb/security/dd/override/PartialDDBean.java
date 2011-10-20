/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.dd.override;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@Stateless
@SecurityDomain("ejb3-tests")
public class PartialDDBean {

    @Resource
    private SessionContext sessionContext;

    @DenyAll
    public void denyAllMethod() {
        throw new RuntimeException("Invocation on this method shouldn't have been allowed!");
    }

    @PermitAll
    public void permitAllMethod() {

    }

    @RolesAllowed("Role1")
    public void toBeInvokedOnlyByRole1() {
    }

    @RolesAllowed("Role1") // Role1 is set here but overriden as Role2 in ejb-jar.xml
    public void toBeInvokedByRole2() {

    }
}
