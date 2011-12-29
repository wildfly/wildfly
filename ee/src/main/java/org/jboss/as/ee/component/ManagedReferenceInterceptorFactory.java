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

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * An interceptor factory which gets an object instance from a managed resource.  A reference to the resource will be
 * attached to the given factory context key; the resource should be retained and passed to an instance of {@link
 * org.jboss.as.ee.component.ManagedReferenceReleaseInterceptorFactory} which is run during destruction.
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
     * @param contextKey             the context key
     */
    public ManagedReferenceInterceptorFactory(final ManagedReferenceFactory componentInstantiation, final Object contextKey) {
        if (componentInstantiation == null) {
            throw MESSAGES.nullVar("componentInstantiation");
        }
        if (contextKey == null) {
            throw MESSAGES.nullVar("contextKey");
        }
        this.componentInstantiation = componentInstantiation;
        this.contextKey = contextKey;
    }

    /**
     * {@inheritDoc}
     */
    public Interceptor create(final InterceptorFactoryContext context) {
        AtomicReference<ManagedReference> referenceReference = (AtomicReference<ManagedReference>) context.getContextData().get(contextKey);
        if (referenceReference == null) {
            referenceReference = new AtomicReference<ManagedReference>();
            context.getContextData().put(contextKey, referenceReference);
        }
        return new ManagedReferenceInterceptor(componentInstantiation, referenceReference);
    }

    static final class ManagedReferenceInterceptor implements Interceptor {

        private final ManagedReferenceFactory componentInstantiator;
        private final AtomicReference<ManagedReference> referenceReference;

        public ManagedReferenceInterceptor(final ManagedReferenceFactory componentInstantiator, final AtomicReference<ManagedReference> referenceReference) {
            this.componentInstantiator = componentInstantiator;
            this.referenceReference = referenceReference;
        }

        public Object processInvocation(final InterceptorContext context) throws Exception {
            final ManagedReference existing = referenceReference.get();
            if (existing == null) {
                final ManagedReference reference = componentInstantiator.getReference();
                boolean ok = false;
                try {
                    referenceReference.set(reference);
                    context.setTarget(reference.getInstance());
                    Object result = context.proceed();
                    ok = true;
                    return result;
                } finally {
                    context.setTarget(null);
                    if (!ok) {
                        reference.release();
                        referenceReference.set(null);
                    }
                }
            } else {
                return context.proceed();
            }
        }
    }
}
