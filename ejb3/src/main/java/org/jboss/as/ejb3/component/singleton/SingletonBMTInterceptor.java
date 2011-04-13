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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.ejb3.tx2.impl.StatelessBMTInterceptor;
import org.jboss.ejb3.tx2.spi.TransactionalComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.transaction.TransactionManager;

/**
 * An {@link Interceptor} for managing Bean Managed Transaction semantics on a
 * singleton bean
 *
 * @author Jaikiran Pai
 */
public class SingletonBMTInterceptor extends StatelessBMTInterceptor implements Interceptor {

    private final SingletonComponent singletonComponent;

    /**
     * @param component The singleton component
     */
    public SingletonBMTInterceptor(SingletonComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("SingletonComponent cannot be null");
        }
        this.singletonComponent = component;
    }

    @Override
    protected TransactionalComponent getTransactionalComponent() {
        return this.singletonComponent;
    }

    @Override
    protected TransactionManager getTransactionManager() {
        return this.singletonComponent.getTransactionManager();
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        return super.invoke(context.getInvocationContext());
    }
}
