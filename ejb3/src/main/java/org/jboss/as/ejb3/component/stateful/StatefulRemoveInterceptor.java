/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.Ejb2xViewType;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor which handles an invocation on a {@link jakarta.ejb.Remove} method of a stateful session bean. This interceptor
 * removes the stateful session once the method completes (either successfully or with an exception). If the remove method
 * was marked with "retainIfException" to true and if the remove method threw a {@link jakarta.ejb.ApplicationException} then
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
        final EJBComponent component = (EJBComponent) context.getPrivateData(Component.class);

        //if a session bean is participating in a transaction, it
        //is an error for a client to invoke the remove method
        //on the session object's home or component interface.
        final ComponentView view = context.getPrivateData(ComponentView.class);
        if (view != null) {
            Ejb2xViewType viewType = view.getPrivateData(Ejb2xViewType.class);
            if (viewType != null) {
                //this means it is an Enterprise Beans 2.x view
                //which is not allowed to remove while enrolled in a TX
                final StatefulTransactionMarker marker = context.getPrivateData(StatefulTransactionMarker.class);
                if (marker != null && !marker.isFirstInvocation()) {
                    throw EjbLogger.ROOT_LOGGER.cannotRemoveWhileParticipatingInTransaction();
                }
            }
        }
        // just log a WARN and throw back the original exception
        if (component instanceof StatefulSessionComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, StatefulSessionComponent.class);
        }
        Object invocationResult = null;
        try {
            // proceed
            invocationResult = context.proceed();
        } catch (Exception e) {
            // Exception caught during call to @Remove method. Handle it appropriately

            // If it's an application exception and if the @Remove method has set "retainIfException" to true
            // then just throw back the exception and don't remove the session instance.
            if (isApplicationException(component, e.getClass(), context.getMethod()) && this.retainIfException) {
                throw e;
            }
            // otherwise, just remove it and throw back the original exception
            remove(context);
            throw e;
        }
        // just remove the session because of a call to @Remove method
        remove(context);
        // return the invocation result
        return invocationResult;
    }

    private static void remove(InterceptorContext context) {
        StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean = StatefulComponentInstanceInterceptor.getBean(context);
        if (bean.isClosed()) {
            // Bean may have been closed by a previous interceptor
            // If so, look it up again
            StatefulSessionComponent component = (StatefulSessionComponent) context.getPrivateData(Component.class);
            SessionID id = context.getPrivateData(SessionID.class);
            bean = component.getCache().findStatefulSessionBean(id);
        }
        if (bean != null) {
            bean.remove();
        }
    }

    /**
     * Returns true if the passed <code>exceptionClass</code> is an application exception. Else returns false.
     *
     * @param ejbComponent   The EJB component
     * @param exceptionClass The exception class
     * @return
     */
    private static boolean isApplicationException(final EJBComponent ejbComponent, final Class<?> exceptionClass, final Method invokedMethod) {
        return ejbComponent.getApplicationException(exceptionClass, invokedMethod) != null;
    }
}
