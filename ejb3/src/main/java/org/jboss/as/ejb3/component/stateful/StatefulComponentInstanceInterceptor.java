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

import java.rmi.RemoteException;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Associate the proper component instance to the invocation based on the passed in session identifier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulComponentInstanceInterceptor extends AbstractEJBInterceptor {


    private static final Logger log = Logger.getLogger(StatefulComponentInstanceInterceptor.class);


    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        // TODO: this is a contract with the client interceptor
        SessionID sessionId = (SessionID) context.getPrivateData(SessionID.class);
        if (sessionId == null) {
            throw MESSAGES.statefulSessionIdIsNull(component.getComponentName());
        }
        ROOT_LOGGER.debug("Looking for stateful component instance with session id: " + sessionId);
        StatefulSessionComponentInstance instance = component.getCache().get(sessionId);
        if (instance == null) {
            //This exception will be transformed into the correct exception type by the exception transforming interceptor
            throw new NoSuchEJBException("Could not find SFSB " + component.getComponentName() + " with " + sessionId);
        }
        try {
            context.putPrivateData(ComponentInstance.class, instance);
            return context.proceed();
        } catch (Exception ex) {
            // Detect app exception
            if (component.getApplicationException(ex.getClass(), context.getMethod()) != null) {
                // it's an application exception, just throw it back.
                throw ex;
            }
            if (ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                throw ex;
            }
            if (!(ex instanceof RemoveException)) {
                if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                    if (ROOT_LOGGER.isTraceEnabled())
                        ROOT_LOGGER.trace("Removing bean " + sessionId + " because of exception", ex);
                    instance.discard();
                }
            }
            throw ex;
        } catch (final Error e) {
            if (ROOT_LOGGER.isTraceEnabled())
                ROOT_LOGGER.trace("Removing bean " + sessionId + " because of error", e);
            instance.discard();
            throw e;
        } catch (final Throwable t) {
            if (ROOT_LOGGER.isTraceEnabled())
                ROOT_LOGGER.trace("Removing bean " + sessionId + " because of Throwable", t);
            instance.discard();
            throw new RuntimeException(t);
        }
    }

    static StatefulSessionComponentInstance getComponentInstance(InterceptorContext context) {
        return (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
    }

    public static class Factory implements InterceptorFactory {

        public static final InterceptorFactory INSTANCE = new Factory();

        private Factory() {
        }

        @Override
        public Interceptor create(InterceptorFactoryContext context) {
            return new StatefulComponentInstanceInterceptor();
        }
    }

}
