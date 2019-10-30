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

import javax.transaction.UserTransaction;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * A special type of {@link BinderService} used to bind {@link javax.transaction.UserTransaction} instances. This
 * {@link UserTransactionBindingService} checks the permission to access the UserTransaction} before handing out the
 * {@link javax.transaction.UserTransaction} instance from its {@link #getValue()} method.
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
