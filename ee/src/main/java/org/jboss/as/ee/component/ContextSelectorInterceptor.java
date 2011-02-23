/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * Naming context selector interceptor used for an EE component.  This will setup the correct namespace selector for the
 * component.
 *
 * @author John Bailey
 */
public class ContextSelectorInterceptor implements Interceptor {
    private final Component component;

    public ContextSelectorInterceptor(final Component component) {
        this.component = component;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext invocationContext) throws Exception {
        NamespaceContextSelector.pushCurrentSelector(component.getNamespaceContextSelector());
        try {
            return invocationContext.proceed();
        } finally {
            NamespaceContextSelector.getCurrentSelector();
        }
    }
}
