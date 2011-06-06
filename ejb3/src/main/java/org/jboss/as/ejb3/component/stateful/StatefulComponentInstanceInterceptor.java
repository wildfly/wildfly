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
package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.AbstractEJBInterceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Associate the proper component instance to the invocation based on the passed in session identifier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulComponentInstanceInterceptor extends AbstractEJBInterceptor {
    private static final Logger log = Logger.getLogger(StatefulComponentInstanceInterceptor.class);

    private final AtomicReference<Serializable> sessionIdReference;

    public StatefulComponentInstanceInterceptor(AtomicReference<Serializable> sessionIdReference) {
        this.sessionIdReference = sessionIdReference;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        // TODO: this is a contract with the client interceptor
        Serializable sessionId = this.sessionIdReference.get();
        if (sessionId == null) {
            throw new IllegalStateException("Session id hasn't been set for stateful component: " + component.getComponentName());
        }
        log.debug("Looking for stateful component instance with session id: " + sessionId);
        StatefulSessionComponentInstance instance = component.getCache().get(sessionId);
        try {
            context.putPrivateData(ComponentInstance.class, instance);
            return context.proceed();
        } catch (Exception ex) {
            // Detect app exception
            if (component.getApplicationException(ex.getClass()) != null) {
                // it's an application exception, just throw it back.
                throw ex;
            }
            if(ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                throw ex;
            }
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                if (log.isTraceEnabled())
                    log.trace("Removing bean " + sessionId + " because of exception", ex);
                component.getCache().discard(sessionId);
            }
            throw ex;
        } catch (final Error e) {
            if (log.isTraceEnabled())
                log.trace("Removing bean " + sessionId + " because of error", e);
            component.getCache().discard(sessionId);
            throw e;
        } catch (final Throwable t) {
            if (log.isTraceEnabled())
                log.trace("Removing bean " + sessionId + " because of Throwable", t);
            component.getCache().discard(sessionId);
            throw new RuntimeException(t);
        } finally {
            // the StatefulSessionSynchronizationInterceptor will take care of releasing
            context.putPrivateData(ComponentInstance.class, null);
        }
    }

    static StatefulSessionComponentInstance getComponentInstance(InterceptorContext context) {
        return (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
    }
}
