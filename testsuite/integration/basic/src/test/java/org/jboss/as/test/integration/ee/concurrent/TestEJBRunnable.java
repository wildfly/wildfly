/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.security.Principal;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

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
