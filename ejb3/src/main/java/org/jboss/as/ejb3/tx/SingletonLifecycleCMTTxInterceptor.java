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
package org.jboss.as.ejb3.tx;

import javax.ejb.TransactionAttributeType;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SingletonLifecycleCMTTxInterceptor extends CMTTxInterceptor implements Interceptor {
    private final TransactionAttributeType txAttr;

    public SingletonLifecycleCMTTxInterceptor( final TransactionAttributeType txAttr) {
        this.txAttr = txAttr;
    }

    @Override
    public Object processInvocation(InterceptorContext invocation) throws Exception {
        final EJBComponent component = (EJBComponent) invocation.getPrivateData(Component.class);
        switch (txAttr) {
            case MANDATORY:
                return mandatory(invocation, component);
            case NEVER:
                return never(invocation, component);
            case NOT_SUPPORTED:
                return notSupported(invocation, component);
            //singleton beans lifecyle methods must treat REQUIRED as REQUIRES_NEW
            case REQUIRED:
            case REQUIRES_NEW:
                return requiresNew(invocation, component, -1);
            case SUPPORTS:
                return supports(invocation, component);
            default:
                throw new IllegalStateException("Unexpected tx attribute " + txAttr + " on " + invocation);
        }
    }

    /**
     * @author Stuart Douglas
     */
    public static class Factory extends ComponentInterceptorFactory {

        private final TransactionAttributeType txAttr;

        public Factory(final TransactionAttributeType txAttr) {
            this.txAttr = txAttr;
        }

        @Override
        protected Interceptor create(Component component, InterceptorFactoryContext context) {
            final SingletonLifecycleCMTTxInterceptor interceptor = new SingletonLifecycleCMTTxInterceptor(txAttr);
            return interceptor;
        }
    }

}
