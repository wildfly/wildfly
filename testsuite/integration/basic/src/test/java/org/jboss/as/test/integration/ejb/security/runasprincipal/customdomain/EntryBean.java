/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.security.runasprincipal.customdomain;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Entry WhoAmI implementation, uses default security domain and calls method on TargetBean which uses another domain.
 *
 * @author Josef Cacek
 * @see TargetBean
 */
@Stateless
@Remote(WhoAmI.class)
@DeclareRoles({ "guest", "Target" })
@RunAs("Target")
@RunAsPrincipal("principalFromEntryBean")
@SecurityDomain("other")
public class EntryBean implements WhoAmI {

    @EJB(beanName = "TargetBean")
    private WhoAmI target;

    @Resource
    private SessionContext ctx;

    @RolesAllowed("guest")
    public String getCallerPrincipal() {
        return target.getCallerPrincipal();
    }
}
