/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Supplier;

import jakarta.transaction.UserTransaction;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.service.BinderService;

/**
 * A special type of {@link BinderService} used to bind {@link jakarta.transaction.UserTransaction} instances. This
 * {@link UserTransactionBindingService} checks the permission to access the UserTransaction} before handing out the
 * {@link jakarta.transaction.UserTransaction} instance from its {@link #getValue()} method.
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UserTransactionBindingService extends BinderService {

    private final Supplier<UserTransactionAccessControlService> accessControlServiceSupplier;

    public UserTransactionBindingService(final Supplier<UserTransactionAccessControlService> accessControlServiceSupplier, final String name) {
        super(name);
        this.accessControlServiceSupplier = accessControlServiceSupplier;
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
                accessControlServiceSupplier.get().authorizeAccess();
                return value.getReference();
            }
        };
    }

}
