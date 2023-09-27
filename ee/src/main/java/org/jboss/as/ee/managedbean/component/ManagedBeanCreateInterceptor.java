/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.managedbean.component;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentClientInstance;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ManagedBeanCreateInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new ManagedBeanCreateInterceptor());

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ComponentClientInstance instance = context.getPrivateData(ComponentClientInstance.class);
        final Component component = context.getPrivateData(Component.class);
        final ComponentInstance componentInstance = component.createInstance();
        boolean ok = false;
        try {
            context.putPrivateData(ComponentInstance.class, componentInstance);
            instance.setViewInstanceData(ComponentInstance.class, componentInstance);
            final Object result = context.proceed();
            ok = true;
            return result;
        } finally {
            context.putPrivateData(ComponentInstance.class, null);
            if (! ok) {
                componentInstance.destroy();
                instance.setViewInstanceData(ComponentInstance.class, null);
            }
        }
    }
}
