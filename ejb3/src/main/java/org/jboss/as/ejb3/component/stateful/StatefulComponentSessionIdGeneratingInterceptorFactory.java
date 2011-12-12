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

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.Component;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * User: jpai
 */
public class StatefulComponentSessionIdGeneratingInterceptorFactory implements InterceptorFactory {

    public static final StatefulComponentSessionIdGeneratingInterceptorFactory INSTANCE = new StatefulComponentSessionIdGeneratingInterceptorFactory();

    private StatefulComponentSessionIdGeneratingInterceptorFactory() {
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        final AtomicReference<SessionID> sessionIdReference = new AtomicReference<SessionID>();
        context.getContextData().put(StatefulSessionComponent.SESSION_ID_REFERENCE_KEY, sessionIdReference);

        //if we are attaching to an existing instance this will not be null
        final SessionID id = (SessionID) context.getContextData().get(SessionID.class);
        if(id == null) {
            return new StatefulComponentSessionIdGeneratingInterceptor(sessionIdReference);
        } else {
            sessionIdReference.set(id);
            return Interceptors.getTerminalInterceptor();
        }
    }

    private class StatefulComponentSessionIdGeneratingInterceptor implements Interceptor {

        private final AtomicReference<SessionID> sessionIdReference;

        StatefulComponentSessionIdGeneratingInterceptor(AtomicReference<SessionID> sessionIdReference) {
            this.sessionIdReference = sessionIdReference;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            final Component component = context.getPrivateData(Component.class);
            if (component instanceof StatefulSessionComponent == false) {
                throw MESSAGES.unexpectedComponent(component,StatefulSessionComponent.class);
            }
            StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
            StatefulSessionComponentInstance statefulSessionComponentInstance = statefulComponent.getCache().create();
            this.sessionIdReference.set(statefulSessionComponentInstance.getId());

            // move to the next interceptor in chain
            return context.proceed();
        }
    }


}
