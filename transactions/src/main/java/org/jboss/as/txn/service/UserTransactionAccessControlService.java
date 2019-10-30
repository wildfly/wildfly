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

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Allows enabling/disabling access to the {@link javax.transaction.UserTransaction} at runtime. Typically, components (like the
 * EJB component), at runtime, based on a certain criteria decide whether or not access to the
 * {@link javax.transaction.UserTransaction} is allowed during an invocation associated with a thread. The
 * {@link UserTransactionService} and the {@link UserTransactionBindingService} which are responsible for handing out the
 * {@link javax.transaction.UserTransaction} use this service to decide whether or not they should hand out the
 * {@link javax.transaction.UserTransaction}
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
public class UserTransactionAccessControlService implements Service<UserTransactionAccessControlService> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN.append("UserTransactionAccessControlService");

    private UserTransactionAccessControl accessControl;

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public UserTransactionAccessControlService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     *
     * @return
     */
    public UserTransactionAccessControl getAccessControl() {
        return accessControl;
    }

    /**
     *
     * @param accessControl
     */
    public void setAccessControl(UserTransactionAccessControl accessControl) {
        this.accessControl = accessControl;
    }

    /**
     * Authorize access of user transaction
     */
    public void authorizeAccess() {
        final UserTransactionAccessControl accessControl = this.accessControl;
        if(accessControl != null) {
            accessControl.authorizeAccess();
        }
    }
}
