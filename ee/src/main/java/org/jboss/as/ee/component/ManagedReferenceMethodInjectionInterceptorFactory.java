/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.InvocationType;
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
final class ManagedReferenceMethodInjectionInterceptorFactory implements InterceptorFactory {

    private final Object targetContextKey;
    private final Object valueContextKey;
    private final Value<ManagedReferenceFactory> factoryValue;
    private final Method method;
    private final boolean optional;

    ManagedReferenceMethodInjectionInterceptorFactory(final Object targetContextKey, final Object valueContextKey, final Value<ManagedReferenceFactory> factoryValue, final Method method, final boolean optional) {
        this.targetContextKey = targetContextKey;
        this.valueContextKey = valueContextKey;
        this.factoryValue = factoryValue;
        this.method = method;
        this.optional = optional;
    }

    public Interceptor create(final InterceptorFactoryContext context) {
        return new ManagedReferenceMethodInjectionInterceptor(targetContextKey, valueContextKey, factoryValue.getValue(), method, optional);
    }

    /**
     * An interceptor which constructs and injects a managed reference into a setter method.  The context key given
     * for storing the reference should be passed to a {@link ManagedReferenceReleaseInterceptor} which is run during
     * object destruction.
     *
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    static final class ManagedReferenceMethodInjectionInterceptor implements Interceptor {

        private final Object targetKey;
        private final Object valueKey;
        private final ManagedReferenceFactory factory;
        private final Method method;
        private final boolean optional;

        ManagedReferenceMethodInjectionInterceptor(final Object targetKey, final Object valueKey, final ManagedReferenceFactory factory, final Method method, final boolean optional) {
            this.targetKey = targetKey;
            this.factory = factory;
            this.method = method;
            this.optional = optional;
            this.valueKey = valueKey;
        }

        /**
         * {@inheritDoc}
         */
        public Object processInvocation(final InterceptorContext context) throws Exception {
            ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
            Object target;
            if (Modifier.isStatic(method.getModifiers())) {
                target = null;
            } else {
                target = ((ManagedReference) componentInstance.getInstanceData(targetKey)).getInstance();
                if (target == null) {
                    throw EeLogger.ROOT_LOGGER.injectionTargetNotFound();
                }
            }
            ManagedReference reference = factory.getReference();
            if (reference == null && optional) {
                return context.proceed();
            }
            if (reference == null) {
                throw EeLogger.ROOT_LOGGER.managedReferenceMethodWasNull(method);
            }

            boolean ok = false;
            try {
                componentInstance.setInstanceData(valueKey, reference);
                final InvocationType invocationType = context.getPrivateData(InvocationType.class);
                try {
                    context.putPrivateData(InvocationType.class, InvocationType.DEPENDENCY_INJECTION);
                    method.invoke(target, reference.getInstance());
                } finally {
                    context.putPrivateData(InvocationType.class, invocationType);
                }
                Object result = context.proceed();
                ok = true;
                return result;
            } finally {
                if (!ok) {
                    componentInstance.setInstanceData(valueKey, null);
                    reference.release();
                }
            }
        }
    }
}
