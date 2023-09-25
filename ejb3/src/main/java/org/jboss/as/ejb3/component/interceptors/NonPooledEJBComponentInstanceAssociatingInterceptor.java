/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.InterceptorContext;

import jakarta.ejb.ConcurrentAccessException;
import jakarta.ejb.ConcurrentAccessTimeoutException;
import java.rmi.RemoteException;

/**
 * A {@link ComponentInstance} associating interceptor for Jakarta Enterprise Beans components (SLSB and message driven) which
 * have pooling disabled. Upon each {@link #processInvocation(org.jboss.invocation.InterceptorContext) invocation}
 * this interceptor creates a new {@link ComponentInstance} and associates it with the invocation. It then
 * {@link org.jboss.as.ee.component.ComponentInstance#destroy() destroys} the instance upon method completion.
 * <p/>
 * User: Jaikiran Pai
 */
public class NonPooledEJBComponentInstanceAssociatingInterceptor extends AbstractEJBInterceptor {

    public static final NonPooledEJBComponentInstanceAssociatingInterceptor INSTANCE = new NonPooledEJBComponentInstanceAssociatingInterceptor();

    private NonPooledEJBComponentInstanceAssociatingInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final EJBComponent component = getComponent(context, EJBComponent.class);
        // create the instance
        final ComponentInstance componentInstance = component.createInstance();
        context.putPrivateData(ComponentInstance.class, componentInstance);
        //if this is set to true we do not invoke instance.destroy
        //as we are not allowed to invoke pre-destroy callbacks
        boolean discard = false;
        try {
            return context.proceed();
        } catch (Exception ex) {
            final EJBComponent ejbComponent = component;
            // Detect app exception
            if (ejbComponent.getApplicationException(ex.getClass(), context.getMethod()) != null) {
                // it's an application exception, just throw it back.
                throw ex;
            }
            if (ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                throw ex;
            }
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                discard = true;
            }
            throw ex;
        } catch (final Error e) {
            discard = true;
            throw e;
        } catch (final Throwable t) {
            discard = true;
            throw new RuntimeException(t);
        } finally {
            // destroy the instance
            if (!discard) {
                componentInstance.destroy();
            }
        }
    }

}
