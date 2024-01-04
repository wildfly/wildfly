/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceName;

/**
 * An interceptor which imposes the given namespace context selector.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NamespaceContextInterceptor implements Interceptor {
    private final NamespaceContextSelector selector;
    private final ServiceName deploymentUnitServiceName;

    public NamespaceContextInterceptor(final NamespaceContextSelector selector, final ServiceName deploymentUnitServiceName) {
        this.selector = selector;
        this.deploymentUnitServiceName = deploymentUnitServiceName;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        NamespaceContextSelector.pushCurrentSelector(selector);
        try {
            WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
            try {
                return context.proceed();
            } finally {
                WritableServiceBasedNamingStore.popOwner();
            }
        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }
}
