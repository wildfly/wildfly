/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.txn.service.UserTransactionAccessControl;
import org.jboss.as.txn.service.UserTransactionAccessControlService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service which installs the {@link javax.transaction.UserTransaction} access control into the transaction subsystem.
 *
 * @author Eduardo Martins
 */
public class EJB3UserTransactionAccessControlService implements Service<EJB3UserTransactionAccessControlService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "EJB3UserTransactionAccessControlService");

    private final InjectedValue<UserTransactionAccessControlService> accessControlService = new InjectedValue<UserTransactionAccessControlService>();

    @Override
    public void start(StartContext context) throws StartException {
        UserTransactionAccessControl accessControl = new UserTransactionAccessControl() {
            @Override
            public void authorizeAccess() {
                AllowedMethodsInformation.checkAllowed(MethodType.GET_USER_TRANSACTION);
            }
        };
        this.accessControlService.getValue().setAccessControl(accessControl);
    }

    @Override
    public void stop(StopContext context) {
        this.accessControlService.getValue().setAccessControl(null);
    }

    @Override
    public EJB3UserTransactionAccessControlService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     *
     * @return
     */
    public Injector<UserTransactionAccessControlService> getUserTransactionAccessControlServiceInjector() {
        return this.accessControlService;
    }
}
