/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.pool;

import java.rmi.RemoteException;

import jakarta.ejb.ConcurrentAccessException;
import jakarta.ejb.ConcurrentAccessTimeoutException;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class PooledInstanceInterceptor extends AbstractEJBInterceptor {
    public static final PooledInstanceInterceptor INSTANCE = new PooledInstanceInterceptor();

    private PooledInstanceInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        PooledComponent<ComponentInstance> component = (PooledComponent<ComponentInstance>) getComponent(context, EJBComponent.class);
        ComponentInstance instance = component.getPool().get();
        context.putPrivateData(ComponentInstance.class, instance);
        boolean discarded = false;
        try {
            return context.proceed();
        } catch (Exception ex) {
            final EJBComponent ejbComponent = (EJBComponent)component;
            // Detect app exception
            if (ejbComponent.getApplicationException(ex.getClass(), context.getMethod()) != null) {
                // it's an application exception, just throw it back.
                throw ex;
            }
            if(ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                throw ex;
            }
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                discarded = true;
                component.getPool().discard(instance);
            }
            throw ex;
        } catch (final Error e) {
            discarded = true;
            component.getPool().discard(instance);
            throw e;
        } catch (final Throwable t) {
            discarded = true;
            component.getPool().discard(instance);
            throw new RuntimeException(t);
        }  finally {
            if (!discarded) {
                component.getPool().release(instance);
            }
        }
    }
}
