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

package org.jboss.as.test.integration.ejb.management.deployments;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Bean to use in tests of management resources for SLSBs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Stateless
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Pool("slsb-strict-max-pool")
public class NoTimerStatelessBean implements BusinessInterface {

    @Override
    public void doIt() {
        // no-op;
    }

    @Override
    public void remove() {
        // no-op;
    }
}
