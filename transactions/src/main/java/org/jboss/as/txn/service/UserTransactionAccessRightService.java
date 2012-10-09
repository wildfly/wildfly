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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.Stack;

/**
 * {@link UserTransactionAccessRightService} allows enabling/disabling access to the {@link javax.transaction.UserTransaction}
 * at runtime. Typically, components (like the EJB component), at runtime, based on a certain criteria decide whether or not
 * access to the {@link javax.transaction.UserTransaction} is allowed during an invocation associated with a thread.
 * The {@link UserTransactionService} and the {@link UserTransactionBindingService} which are responsible for handing
 * out the {@link javax.transaction.UserTransaction} use this {@link UserTransactionAccessRightService} to decide whether
 * or not they should hand out the {@link javax.transaction.UserTransaction}
 *
 * @author Jaikiran Pai
 */
public class UserTransactionAccessRightService implements Service<UserTransactionAccessRightService> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN.append("UserTransactionAccessRightService");

    /**
     * Permissions for access to P{@link javax.transaction.UserTransaction}. {@link #ALLOWED} will
     * allow access whereas {@link #DISALLOWED} will disallow access to {@link javax.transaction.UserTransaction}
     */
    public enum UserTransactionAccessPermission {
        ALLOWED,
        DISALLOWED
    }

    // The stack of current permissions associated with the thread
    private final ThreadLocal<Stack<UserTransactionAccessPermission>> currentPermissions = new ThreadLocal<Stack<UserTransactionAccessPermission>>() {
        @Override
        protected Stack<UserTransactionAccessPermission> initialValue() {
            return new Stack<UserTransactionAccessPermission>();
        }
    };

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public UserTransactionAccessRightService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * "Pushes" the passed {@link UserTransactionAccessPermission permission} on to a stack of permissions
     * for the current thread. This permission will then be associated with this thread, until someone invokes
     * this method again or invokes the {@link #popAccessPermission()} method to change the permission
     *
     * @param permission
     */
    public void pushAccessPermission(final UserTransactionAccessPermission permission) {
        final Stack<UserTransactionAccessPermission> permissions = this.currentPermissions.get();
        permissions.push(permission);
    }

    /**
     * "Pops" the current {@link UserTransactionAccessPermission permission} that's associated with the thread.
     *
     * @return
     */
    public UserTransactionAccessPermission popAccessPermission() {
        final Stack<UserTransactionAccessPermission> permissions = this.currentPermissions.get();
        return permissions.pop();
    }

    /**
     * Returns true if the current thread is associated with a {@link UserTransactionAccessPermission#DISALLOWED}
     * permission. Else returns false in all other cases.
     *
     * @return
     */
    public boolean isUserTransactionAccessDisAllowed() {
        final Stack<UserTransactionAccessPermission> permissions = this.currentPermissions.get();
        if (permissions.isEmpty()) {
            return false;
        }
        return permissions.peek() == UserTransactionAccessPermission.DISALLOWED;
    }

}
