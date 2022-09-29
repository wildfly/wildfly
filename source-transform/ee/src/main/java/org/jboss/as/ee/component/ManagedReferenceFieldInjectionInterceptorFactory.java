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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceFieldInjectionInterceptorFactory implements InterceptorFactory {

    private final Object targetContextKey;
    private final Object valueContextKey;
    private final Value<ManagedReferenceFactory> factoryValue;
    private final Field field;
    private final boolean optional;

    ManagedReferenceFieldInjectionInterceptorFactory(final Object targetContextKey, final Object valueContextKey, final Value<ManagedReferenceFactory> factoryValue, final Field field, final boolean optional) {
        this.targetContextKey = targetContextKey;
        this.valueContextKey = valueContextKey;
        this.factoryValue = factoryValue;
        this.field = field;
        this.optional = optional;
    }

    public Interceptor create(final InterceptorFactoryContext context) {
        return new ManagedReferenceFieldInjectionInterceptor(targetContextKey, valueContextKey, factoryValue.getValue(), field, optional);
    }

    /**
     * An interceptor which constructs and injects a managed reference into a field.  The context key given
     * for storing the reference should be passed to a {@link ManagedReferenceReleaseInterceptor} which is run during
     * object destruction.
     *
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    static final class ManagedReferenceFieldInjectionInterceptor implements Interceptor {

        private final Object targetKey;
        private final ManagedReferenceFactory factory;
        private final Field field;
        private final boolean optional;
        private final Object valueContextKey;

        ManagedReferenceFieldInjectionInterceptor(final Object targetKey, final Object valueContextKey, final ManagedReferenceFactory factory, final Field field, final boolean optional) {
            this.targetKey = targetKey;
            this.factory = factory;
            this.field = field;
            this.optional = optional;
            this.valueContextKey = valueContextKey;
        }

        /**
         * {@inheritDoc}
         */
        public Object processInvocation(final InterceptorContext context) throws Exception {
            ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
            Object target;
            if (Modifier.isStatic(field.getModifiers())) {
                target = null;
            } else {
                target = ((ManagedReference) componentInstance.getInstanceData(targetKey)).getInstance();
                if (target == null) {
                    throw EeLogger.ROOT_LOGGER.injectionTargetNotFound();
                }
            }
            final ManagedReference reference = factory.getReference();
            if (reference == null && optional) {
                return context.proceed();
            } else if(reference == null) {
                throw EeLogger.ROOT_LOGGER.managedReferenceWasNull(field);
            }
            boolean ok = false;
            try {
                componentInstance.setInstanceData(valueContextKey, reference);
                Object injected = reference.getInstance();
                try {
                    field.set(target, injected);
                } catch (IllegalArgumentException e) {
                    throw EeLogger.ROOT_LOGGER.cannotSetField(field.getName(), injected.getClass(), injected.getClass().getClassLoader(), field.getType(), field.getType().getClassLoader());
                }
                Object result = context.proceed();
                ok = true;
                return result;
            } finally {
                if (!ok) {
                    componentInstance.setInstanceData(valueContextKey, null);
                    reference.release();
                }
            }
        }
    }

}
