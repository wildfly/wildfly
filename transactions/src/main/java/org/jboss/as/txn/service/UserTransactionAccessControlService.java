/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;

import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionAccessControlService implements Service {
    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN.append("UserTransactionAccessControlService");
    private final Consumer<UserTransactionAccessControlService> txnAccessControlServiceConsumer;
    private UserTransactionAccessControl accessControl;

    private UserTransactionAccessControlService(final Consumer<UserTransactionAccessControlService> txnAccessControlServiceConsumer) {
        this.txnAccessControlServiceConsumer = txnAccessControlServiceConsumer;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        txnAccessControlServiceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        txnAccessControlServiceConsumer.accept(null);
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

    public static void addService(final CapabilityServiceTarget target) {
        final ServiceBuilder<?> sb = target.addService();
        final Consumer<UserTransactionAccessControlService> txnAccessControlServiceConsumer = sb.provides(UserTransactionAccessControlService.SERVICE_NAME);
        sb.setInstance(new UserTransactionAccessControlService(txnAccessControlServiceConsumer));
        sb.install();
    }
}
