/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.tx;

import javax.ejb.EJBException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

/**
 * EJB 3 13.6.1:
 * If a stateless session bean instance starts a transaction in a business method or interceptor method, it
 * must commit the transaction before the business method (or all its interceptor methods) returns.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StatelessBMTInterceptor extends BMTInterceptor {
    private static final Logger log = Logger.getLogger(StatelessBMTInterceptor.class);

    public StatelessBMTInterceptor(final EJBComponent component) {
        super(component);
    }

    private void checkStatelessDone(final EJBComponent component,final InterceptorContext invocation, final TransactionManager tm, Exception ex) throws Exception {
        int status = Status.STATUS_NO_TRANSACTION;

        try {
            status = tm.getStatus();
        } catch (SystemException sex) {
            log.error("Failed to get status", sex);
        }

        switch (status) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_PREPARING:
            case Status.STATUS_ROLLING_BACK:
                try {
                    tm.rollback();
                } catch (Exception sex) {
                    log.error("Failed to rollback", sex);
                }
                // fall through...
            case Status.STATUS_PREPARED:
                String msg = "EJB 3.1 FR 13.3.3: BMT bean " + component.getComponentName()
                        + " should complete transaction before returning.";
                log.error(msg);
                throw new EJBException(msg, ex);
        }
        // the instance interceptor will discard the instance
        if (ex != null)
            throw this.handleException(invocation, ex);
    }


    @Override
    protected Object handleInvocation(final InterceptorContext invocation) throws Exception {
        final EJBComponent ejbComponent = getComponent();
        TransactionManager tm = ejbComponent.getTransactionManager();
        assert tm.getTransaction() == null : "can't handle BMT transaction, there is a transaction active";

        boolean exceptionThrown = false;
        try {
            return invocation.proceed();
        } catch (Exception ex) {
            exceptionThrown = true;
            checkStatelessDone(ejbComponent, invocation, tm, ex);
            throw ex;
        } finally {
            try {
                if (!exceptionThrown) checkStatelessDone(ejbComponent, invocation, tm, null);
            } finally {
                tm.suspend();
            }
        }
    }
}
