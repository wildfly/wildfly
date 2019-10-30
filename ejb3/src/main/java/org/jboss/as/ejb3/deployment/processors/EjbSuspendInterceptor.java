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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * An interceptor that allows the component to shutdown gracefully.
 *
 * @author Stuart Douglas
 * @author Flavia Rainone
 */
public class EjbSuspendInterceptor extends AbstractEJBInterceptor {

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        InvocationType invocation = context.getPrivateData(InvocationType.class);
        if (invocation != InvocationType.REMOTE && invocation != InvocationType.MESSAGE_DELIVERY) {
            return context.proceed();
        }
        // see if control point accepts or rejects this invocation
        EJBComponent component = getComponent(context, EJBComponent.class);
        ControlPoint entryPoint = component.getControlPoint();
        RunResult result = entryPoint.beginRequest();
        if (result == RunResult.REJECTED) {
            // if control point rejected, check with suspend handler
            if (!component.getEjbSuspendHandlerService().acceptInvocation(context))
                throw EjbLogger.ROOT_LOGGER.containerSuspended();
        }
        try {
            return context.proceed();
        } finally {
            if (result == RunResult.REJECTED)
                component.getEjbSuspendHandlerService().invocationComplete();
            else
                entryPoint.requestComplete();
        }
    }
}
