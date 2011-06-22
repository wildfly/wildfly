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
import org.jboss.invocation.InterceptorContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An interceptor which constructs and injects a managed reference into a setter method.  The context key given
 * for storing the reference should be passed to a {@link org.jboss.as.ee.component.ManagedReferenceReleaseInterceptor} which is run during
 * object destruction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceMethodInjectionInterceptor implements Interceptor {

    private final AtomicReference<ManagedReference> targetReference;
    private final AtomicReference<ManagedReference> valueReference;
    private final ManagedReferenceFactory factory;
    private final Method method;

    ManagedReferenceMethodInjectionInterceptor(final AtomicReference<ManagedReference> targetReference, final AtomicReference<ManagedReference> valueReference, final ManagedReferenceFactory factory, final Method method) {
        this.targetReference = targetReference;
        this.valueReference = valueReference;
        this.factory = factory;
        this.method = method;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        Object target = targetReference.get().getInstance();
        if (target == null) {
            throw new IllegalStateException("No injection target found");
        }
        ManagedReference reference = factory.getReference();
        boolean ok = false;
        try {
            valueReference.set(reference);
            method.invoke(target, reference.getInstance());
            Object result = context.proceed();
            ok = true;
            return result;
        } finally {
            if (!ok) {
                valueReference.set(null);
                reference.release();
            }
        }
    }
}
