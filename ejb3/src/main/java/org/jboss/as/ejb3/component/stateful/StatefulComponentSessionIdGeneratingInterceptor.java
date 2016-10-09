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
            StatefulSessionComponentInstance statefulSessionComponentInstance = statefulComponent.getCache().create();
            clientInstance.setViewInstanceData(SessionID.class, statefulSessionComponentInstance.getId());
        }

        // move to the next interceptor in chain
        return context.proceed();
    }
}
