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
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: jpai
 */
public class StatefulComponentSessionIdGeneratingInterceptorFactory implements InterceptorFactory {

    private final Object sessionIdContextKey;

    public StatefulComponentSessionIdGeneratingInterceptorFactory(Object sessionIdContextKey) {
        this.sessionIdContextKey = sessionIdContextKey;
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        AtomicReference<Serializable> sessionIdReference = new AtomicReference<Serializable>();
        context.getContextData().put(this.sessionIdContextKey, sessionIdReference);
        return new StatefulComponentSessionIdGeneratingInterceptor(sessionIdReference);
    }

    private class StatefulComponentSessionIdGeneratingInterceptor implements Interceptor {

        private AtomicReference<Serializable> sessionIdReference;

        StatefulComponentSessionIdGeneratingInterceptor(AtomicReference<Serializable> sessionIdReference) {
            this.sessionIdReference = sessionIdReference;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            final Component component = (Component) context.getPrivateData(Component.class);
            if (component instanceof StatefulSessionComponent == false) {
                throw new IllegalStateException("Unexpected component: " + component + " Expected " + StatefulSessionComponent.class);
            }
            StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
            StatefulSessionComponentInstance statefulSessionComponentInstance = statefulComponent.getCache().create();
            this.sessionIdReference.set(statefulSessionComponentInstance.getId());

            // move to the next interceptor in chain
            return context.proceed();
        }
    }
}
