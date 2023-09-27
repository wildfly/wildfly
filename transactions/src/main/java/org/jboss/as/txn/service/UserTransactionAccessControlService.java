/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Allows enabling/disabling access to the {@link jakarta.transaction.UserTransaction} at runtime. Typically, components (like the
 * Jakarta Enterprise Beans component), at runtime, based on a certain criteria decide whether or not access to the
 * {@link jakarta.transaction.UserTransaction} is allowed during an invocation associated with a thread. The
 * {@link UserTransactionService} and the {@link UserTransactionBindingService} which are responsible for handing out the
 * {@link jakarta.transaction.UserTransaction} use this service to decide whether or not they should hand out the
 * {@link jakarta.transaction.UserTransaction}
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
