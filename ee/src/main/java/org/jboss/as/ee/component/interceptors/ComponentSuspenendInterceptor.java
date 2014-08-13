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

package org.jboss.as.ee.component.interceptors;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.requestcontroller.ControlPoint;
import org.jboss.as.server.requestcontroller.RunResult;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A per deployment interceptor that allows the component to shutdown gracefully.
 *
 * @author Stuart Douglas
 */
public class ComponentSuspenendInterceptor implements Interceptor {

    private final InjectedValue<ControlPoint> entryPointInjectedValue = new InjectedValue<>();

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        ControlPoint entryPoint = entryPointInjectedValue.getValue();
        RunResult result = entryPoint.beginRequest();
        if (result == RunResult.REJECTED) {
            throw EeLogger.ROOT_LOGGER.componentSuspended();
        }
        try {
            return context.proceed();
        } finally {
            entryPoint.requestComplete();
        }
    }

    public InjectedValue<ControlPoint> getEntryPointInjectedValue() {
        return entryPointInjectedValue;
    }
}
