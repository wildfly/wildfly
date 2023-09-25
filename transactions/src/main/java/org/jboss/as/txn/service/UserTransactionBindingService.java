/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import jakarta.transaction.UserTransaction;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * A special type of {@link BinderService} used to bind {@link jakarta.transaction.UserTransaction} instances. This
 * {@link UserTransactionBindingService} checks the permission to access the UserTransaction} before handing out the
 * {@link jakarta.transaction.UserTransaction} instance from its {@link #getValue()} method.
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
public class UserTransactionBindingService extends BinderService {

    private final InjectedValue<UserTransactionAccessControlService> accessControlService = new InjectedValue<UserTransactionAccessControlService>();

    public UserTransactionBindingService(final String name) {
        super(name);
    }

    @Override
    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException {
        final ManagedReferenceFactory value = super.getValue();
        if (value == null) {
            return null;
        }
        // wrap the real factory in the one that controls access
        return new ContextListAndJndiViewManagedReferenceFactory() {

            @Override
            public String getJndiViewInstanceValue() {
                return UserTransaction.class.getSimpleName();
            }

            @Override
            public String getInstanceClassName() {
                return UserTransaction.class.getName();
            }

            @Override
            public ManagedReference getReference() {
                accessControlService.getValue().authorizeAccess();
                return value.getReference();
            }
        };
    }

    public Injector<UserTransactionAccessControlService> getUserTransactionAccessControlServiceInjector() {
        return accessControlService;
    }

}
