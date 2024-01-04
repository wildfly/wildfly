/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import java.io.Serializable;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.InterceptorContext;
import org.jboss.weld.construction.api.ConstructionHandle;

/**
 * Managed reference factory that can be used to create and inject components.
 *
 * @author Stuart Douglas
 */
public class WeldManagedReferenceFactory implements ComponentFactory {

    public static final WeldManagedReferenceFactory INSTANCE = new WeldManagedReferenceFactory();

    private WeldManagedReferenceFactory() {
    }

    @Override
    public ManagedReference create(final InterceptorContext context) {
        final ConstructionHandle<?> ctx = context.getPrivateData(ConstructionHandle.class);
        final WeldInjectionContext weldCtx = context.getPrivateData(WeldInjectionContext.class);
        if (ctx != null) {
            // @AroundConstructor interception enabled
            Object instance = ctx.proceed(context.getParameters(), context.getContextData()); // let Weld create the instance now
            return new WeldManagedReference(weldCtx.getContext(), instance);
        } else {
            // @AroundConstructor interception handled by Weld alone - no integration with Jakarta Enterprise Beans interceptors
            return new WeldManagedReference(weldCtx.getContext(), weldCtx.produce());
        }
    }

    private static final class WeldManagedReference implements ManagedReference, Serializable {
        private final CreationalContext<?> context;
        private final Object instance;

        private WeldManagedReference(final CreationalContext<?> context, final Object instance) {
            this.context = context;
            this.instance = instance;
        }

        @Override
        public void release() {
            context.release();
        }

        @Override
        public Object getInstance() {
            return instance;
        }
    }
}
