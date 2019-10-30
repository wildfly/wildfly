/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.injection;

import java.io.Serializable;

import javax.enterprise.context.spi.CreationalContext;

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
            // @AroundConstructor interception handled by Weld alone - no integration with EJB interceptors
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
