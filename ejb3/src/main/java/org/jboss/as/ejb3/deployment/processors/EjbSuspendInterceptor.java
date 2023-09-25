/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        if (result == RunResult.REJECTED
                && !component.getEjbSuspendHandlerService().acceptInvocation(context)) {
            // if control point rejected, check with suspend handler
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
