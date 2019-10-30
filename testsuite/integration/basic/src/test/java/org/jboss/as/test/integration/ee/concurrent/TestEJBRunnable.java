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

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * @author Eduardo Martins
 */
public class TestEJBRunnable implements Runnable {

    private Principal expectedPrincipal;

    public void setExpectedPrincipal(Principal expectedPrincipal) {
        this.expectedPrincipal = expectedPrincipal;
    }

    @Override
    public void run() {
        // asserts correct class loader is set
        try {
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // asserts correct naming context is set
        final InitialContext initialContext;
        try {
            initialContext = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        final EJBContext ejbContext;
        try {
            ejbContext = (SessionContext) initialContext.lookup("java:comp/EJBContext");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        // asserts correct security context is set
        final Principal callerPrincipal = ejbContext.getCallerPrincipal();
        if (expectedPrincipal != null) {
            if (!expectedPrincipal.equals(callerPrincipal)) {
                throw new IllegalStateException("the caller principal " + callerPrincipal + " is not the expected " + expectedPrincipal);
            }
        } else {
            if (callerPrincipal != null) {
                throw new IllegalStateException("the caller principal " + callerPrincipal + " is not the expected " + expectedPrincipal);
            }
        }
        // assert tx context is set
        try {
            final UserTransaction userTransaction = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
            userTransaction.begin();
            userTransaction.rollback();
        } catch (NamingException | SystemException | NotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
