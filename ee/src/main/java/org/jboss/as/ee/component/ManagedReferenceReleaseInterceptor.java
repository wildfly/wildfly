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
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An interceptor which releases a managed reference.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ManagedReferenceReleaseInterceptor implements Interceptor {
    private final AtomicReference<ManagedReference> referenceReference;

    /**
     * Construct a new instance.
     *
     * @param referenceReference the holder for the managed reference
     */
    public ManagedReferenceReleaseInterceptor(final AtomicReference<ManagedReference> referenceReference) {
        if (referenceReference == null) {
            throw new IllegalArgumentException("referenceReference is null");
        }
        this.referenceReference = referenceReference;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        try {
            return context.proceed();
        } finally {
            final ManagedReference managedReference = referenceReference.getAndSet(null);
            if (managedReference != null) {
                managedReference.release();
            }
        }
    }
}
