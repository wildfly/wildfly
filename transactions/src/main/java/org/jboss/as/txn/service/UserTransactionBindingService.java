/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.txn.TransactionMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * A special type of {@link BinderService} used to bind {@link javax.transaction.UserTransaction} instances.
 * This {@link UserTransactionBindingService} checks the
 * {@link org.jboss.as.txn.service.UserTransactionAccessRightService#isUserTransactionAccessDisAllowed() permission to access the UserTransaction}
 * before handing out the {@link javax.transaction.UserTransaction} instance from its {@link #getValue()} method.
 * If the caller is {@link org.jboss.as.txn.service.UserTransactionAccessRightService.UserTransactionAccessPermission#DISALLOWED}
 * access to the {@link javax.transaction.UserTransaction} when {@link #getValue()} is called, then this service throws
 * an exception
 *
 * @author Jaikiran Pai
 */
public class UserTransactionBindingService extends BinderService {

    private final InjectedValue<UserTransactionAccessRightService> userTxAccessRights = new InjectedValue<UserTransactionAccessRightService>();

    public UserTransactionBindingService(final String name) {
        super(name);
    }

    @Override
    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException {
        // check if access to UserTransaction is disallowed. If so, throw an exception
        final boolean accessDisallowed = userTxAccessRights.getValue().isUserTransactionAccessDisAllowed();
        if (accessDisallowed) {
            throw TransactionMessages.MESSAGES.userTransactionAccessNotAllowed();
        }
        return super.getValue();
    }

    public Injector<UserTransactionAccessRightService> getUserTransactionAccessRightServiceInjector() {
        return this.userTxAccessRights;
    }
}
