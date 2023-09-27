/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * Service which installs the {@link jakarta.transaction.UserTransaction} access control into the transaction subsystem.
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
