/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentClientInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * User: jpai
 */
public class StatefulComponentSessionIdGeneratingInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new org.jboss.as.ejb3.component.stateful.StatefulComponentSessionIdGeneratingInterceptor());

    private StatefulComponentSessionIdGeneratingInterceptor() {
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof StatefulSessionComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, StatefulSessionComponent.class);
        }
        ComponentClientInstance clientInstance = context.getPrivateData(ComponentClientInstance.class);
        SessionID existing = context.getPrivateData(SessionID.class);
        if (existing != null) {
            clientInstance.setViewInstanceData(SessionID.class, existing);
        } else {
            StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
            statefulComponent.waitForComponentStart();
            clientInstance.setViewInstanceData(SessionID.class, statefulComponent.createSession());
        }

        // move to the next interceptor in chain
        return context.proceed();
    }
}
