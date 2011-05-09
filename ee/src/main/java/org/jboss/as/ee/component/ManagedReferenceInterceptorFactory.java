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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An interceptor factory which gets an object instance from a managed resource.  A reference to the resource will be
 * attached to the given factory context key; the resource should be retained and passed to an instance of {@link
 * org.jboss.as.ee.component.ManagedReferenceReleaseInterceptor} which is run during destruction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ManagedReferenceInterceptorFactory implements InterceptorFactory {

    private final ManagedReferenceFactory componentInstantiation;
    private final Object contextKey;

    /**
     * Construct a new instance.
     *
     * @param componentInstantiation the managed reference factory to create from
     * @param contextKey the context key
     */
    public ManagedReferenceInterceptorFactory(final ManagedReferenceFactory componentInstantiation, final Object contextKey) {
        if (componentInstantiation == null) {
            throw new IllegalArgumentException("componentInstantiation is null");
        }
        if (contextKey == null) {
            throw new IllegalArgumentException("contextKey is null");
        }
        this.componentInstantiation = componentInstantiation;
        this.contextKey = contextKey;
    }

    /** {@inheritDoc} */
    public Interceptor create(final InterceptorFactoryContext context) {
        final AtomicReference<ManagedReference> referenceReference = new AtomicReference<ManagedReference>();
        context.getContextData().put(contextKey, referenceReference);
        return new ManagedReferenceInterceptor(componentInstantiation, referenceReference);
    }
}
