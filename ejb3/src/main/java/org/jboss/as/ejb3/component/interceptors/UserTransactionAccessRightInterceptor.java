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

package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.txn.service.UserTransactionAccessRightService;
import org.jboss.invocation.InterceptorContext;

/**
 * An {@link org.jboss.invocation.Interceptor} which toggles access to {@link javax.transaction.UserTransaction}
 * for EJB components based on the type of the EJB component and the transaction semantics of the EJB component
 * This interceptor is here to take care of the EJB spec requirement that only BMT session and message driven beans
 * are allowed access to {@link javax.transaction.UserTransaction} and CMT and entity beans aren't allowed access to it.
 *
 * @author Jaikiran Pai
 */
public class UserTransactionAccessRightInterceptor extends AbstractEJBInterceptor {

    private final UserTransactionAccessRightService userTxAccessRightService;
    private final boolean userTxAcessAllowed;

    public UserTransactionAccessRightInterceptor(final UserTransactionAccessRightService userTxAccessRightService, final boolean userTxAcessAllowed) {
        this.userTxAccessRightService = userTxAccessRightService;
        this.userTxAcessAllowed = userTxAcessAllowed;
    }


    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        // Push the appropriate permission for UserTransaction access for the current thread
        if (this.userTxAcessAllowed) {
            this.userTxAccessRightService.pushAccessPermission(UserTransactionAccessRightService.UserTransactionAccessPermission.ALLOWED);
        } else {
            this.userTxAccessRightService.pushAccessPermission(UserTransactionAccessRightService.UserTransactionAccessPermission.DISALLOWED);
        }
        try {
            return context.proceed();
        } finally {
            // done with the execution, so let's pop the current permission associated with this thread
            this.userTxAccessRightService.popAccessPermission();
        }
    }
}
