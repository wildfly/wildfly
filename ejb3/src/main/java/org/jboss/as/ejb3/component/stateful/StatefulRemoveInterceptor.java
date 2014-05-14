/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.Ejb2xViewType;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor which handles an invocation on a {@link javax.ejb.Remove} method of a stateful session bean. This interceptor
 * removes the stateful session once the method completes (either successfully or with an exception). If the remove method
 * was marked with "retainIfException" to true and if the remove method threw a {@link javax.ejb.ApplicationException} then
 * this interceptor does *not* remove the stateful session.
 * <p/>
 * User: Jaikiran Pai
 */
public class StatefulRemoveInterceptor implements Interceptor {

    private final boolean retainIfException;

    public StatefulRemoveInterceptor(final boolean retainIfException) {
        this.retainIfException = retainIfException;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);

        //if a session bean is participating in a transaction, it
        //is an error for a client to invoke the remove method
        //on the session object's home or component interface.
        final ComponentView view = context.getPrivateData(ComponentView.class);
        if (view != null) {
            Ejb2xViewType viewType = view.getPrivateData(Ejb2xViewType.class);
            if (viewType != null) {
                //this means it is an EJB 2.x view
                //which is not allowed to remove while enrolled in a TX
                final StatefulTransactionMarker marker = context.getPrivateData(StatefulTransactionMarker.class);
                if(marker != null && !marker.isFirstInvocation()) {
                    throw EjbLogger.ROOT_LOGGER.cannotRemoveWhileParticipatingInTransaction();
                }
            }
        }
        // just log a WARN and throw back the original exception
        if (component instanceof StatefulSessionComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, StatefulSessionComponent.class);
        }
        final StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
        Object invocationResult = null;
        try {
            // proceed
            invocationResult = context.proceed();
        } catch (Exception e) {
            // Exception caught during call to @Remove method. Handle it appropriately

            // If it's an application exception and if the @Remove method has set "retainIfException" to true
            // then just throw back the exception and don't remove the session instance.
            if (this.isApplicationException(statefulComponent, e.getClass(), context.getMethod()) && this.retainIfException) {
                throw e;
            }
            // otherwise, just remove it and throw back the original exception
            final StatefulSessionComponentInstance statefulComponentInstance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
            final SessionID sessionId = statefulComponentInstance.getId();
            statefulComponent.removeSession(sessionId);
            throw e;
        }
        final StatefulSessionComponentInstance statefulComponentInstance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
        final SessionID sessionId = statefulComponentInstance.getId();
        // just remove the session because of a call to @Remove method
        statefulComponent.removeSession(sessionId);
        // return the invocation result
        return invocationResult;
    }

    /**
     * Returns true if the passed <code>exceptionClass</code> is an application exception. Else returns false.
     *
     * @param ejbComponent   The EJB component
     * @param exceptionClass The exception class
     * @return
     */
    private boolean isApplicationException(final EJBComponent ejbComponent, final Class<?> exceptionClass, final Method invokedMethod) {
        return ejbComponent.getApplicationException(exceptionClass, invokedMethod) != null;
    }


}
