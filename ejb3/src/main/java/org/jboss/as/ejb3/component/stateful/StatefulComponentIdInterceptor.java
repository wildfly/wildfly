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

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

/**
 * Client interceptor that associates a SFSB id with an invocation
 *
 * @author Stuart Douglas
 */
public class StatefulComponentIdInterceptor extends AbstractEJBInterceptor {
    private static final Logger log = Logger.getLogger(StatefulComponentIdInterceptor.class);

    private final AtomicReference<SessionID> reference;

    public StatefulComponentIdInterceptor(AtomicReference<SessionID> reference) {
        this.reference = reference;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        try {
            context.putPrivateData(SessionID.class, reference.get());
            return context.proceed();
        } finally {
            context.putPrivateData(SessionID.class, null);
        }
    }

    public static class Factory implements InterceptorFactory {

        public static final InterceptorFactory INSTANCE = new Factory();

        private Factory() {
        }

        @Override
        public Interceptor create(InterceptorFactoryContext context) {
            final AtomicReference<SessionID> reference = (AtomicReference<SessionID>) context.getContextData().get(StatefulSessionComponent.SESSION_ID_REFERENCE_KEY);
            return new StatefulComponentIdInterceptor(reference);
        }
    }

}
