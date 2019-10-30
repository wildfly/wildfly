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

package org.jboss.as.ejb3.component.interceptors;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceName;

/**
 * Logs any exceptions/errors that happen during invocation of EJB methods, as specified by the
 * EJB3 spec, section 14.3
 * <p/>
 * Note: This should be the near the start of interceptor in the chain of interceptors, for this to be able to
 * catch all kinds of errors/exceptions.
 *
 * It should also be run inside the exception transforming interceptor, to make sure that the original exception is logged
 *
 * @author Jaikiran Pai
 */
public class LoggingInterceptor implements Interceptor {

    public static final ServiceName LOGGING_ENABLED_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "logging", "enabled");

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new LoggingInterceptor());

    private LoggingInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        final EJBComponent component = (EJBComponent) interceptorContext.getPrivateData(Component.class);
        if(!component.isExceptionLoggingEnabled()) {
            return interceptorContext.proceed();
        }
        try {
            // we just pass on the control and do our work only when an exception occurs
            return interceptorContext.proceed();
        } catch ( EJBComponentUnavailableException ex) {
            if ( EjbLogger.EJB3_INVOCATION_LOGGER.isTraceEnabled() )
                EjbLogger.EJB3_INVOCATION_LOGGER.trace(ex.getMessage());
            throw ex;
        } catch (Throwable t) {
            final Method invokedMethod = interceptorContext.getMethod();
            // check if it's an application exception. If yes, then *don't* log
            final ApplicationExceptionDetails appException = component.getApplicationException(t.getClass(), invokedMethod);
            if (appException == null) {
                EjbLogger.EJB3_INVOCATION_LOGGER.invocationFailed(component.getComponentName(), invokedMethod, t);
            }
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            // Typically, this interceptor (which would be the first one in the chain) would catch Exception and not Throwable since the other EJB interceptors
            // down the chain would have already wrapped the Throwable accordingly. However, if for some reason,
            // the failure happened even before those interceptors could come into play, then we just wrap the throwable
            // here and return it as an exception
            throw new Exception(t);
        }
    }
}
