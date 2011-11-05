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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Creates and reutrns a {@link EJBRemoteTransactionPropogatingInterceptor}
 *
 * @author Jaikiran Pai
 */
class EJBRemoteTransactionPropogatingInterceptorFactory extends ComponentInterceptorFactory {

    static final EJBRemoteTransactionPropogatingInterceptorFactory INSTANCE = new EJBRemoteTransactionPropogatingInterceptorFactory();

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (!(component instanceof EJBComponent)) {
            throw new IllegalArgumentException(component + " is not an EJB component");
        }
        final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository = ((EJBComponent) component).getEjbRemoteTransactionsRepository();
        return new EJBRemoteTransactionPropogatingInterceptor(ejbRemoteTransactionsRepository);
    }
}
