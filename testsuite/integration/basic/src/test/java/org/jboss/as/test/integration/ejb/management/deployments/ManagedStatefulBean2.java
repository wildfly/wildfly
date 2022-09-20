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

import java.util.concurrent.TimeUnit;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.AfterBegin;
import jakarta.ejb.AfterCompletion;
import jakarta.ejb.BeforeCompletion;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.ejb3.annotation.SecurityDomain;

@Stateful(passivationCapable = false)
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Cache("distributable")
@StatefulTimeout(value = 2, unit = TimeUnit.HOURS)
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class ManagedStatefulBean2 extends AbstractManagedBean implements BusinessInterface {
    @AfterBegin
    private void afterBegin() {
    }

    @BeforeCompletion
    private void beforeCompletion() {
    }

    @AfterCompletion
    private void afterCompletion() {
    }

    @Remove(retainIfException = true)
    public void removeTrue() {
    }

    @Remove(retainIfException = false)
    public void removeFalse() {
    }
}
