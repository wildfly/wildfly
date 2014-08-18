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
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * An interceptor that allows the component to shutdown gracefully.
 *
 * @author Stuart Douglas
 */
public class EjbRemoteSuspendInterceptor implements Interceptor {

    private final InjectedValue<ControlPoint> controlPointInjectedValue = new InjectedValue<>();

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        InvocationType invocation = context.getPrivateData(InvocationType.class);
        if(invocation != InvocationType.REMOTE) {
            return context.proceed();
        }
        ControlPoint entryPoint = controlPointInjectedValue.getValue();
        RunResult result = entryPoint.beginRequest();
        if (result == RunResult.REJECTED) {
            throw EjbLogger.ROOT_LOGGER.containerSuspended();
        }
        try {
            return context.proceed();
        } finally {
            entryPoint.requestComplete();
        }
    }

    public InjectedValue<ControlPoint> getControlPointInjectedValue() {
        return controlPointInjectedValue;
    }
}
