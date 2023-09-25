/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.interceptors;

import jakarta.ejb.Handle;

import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 *
 *
 * @author Stuart Douglas
 */
public class HomeRemoveInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new HomeRemoveInterceptor());

    private HomeRemoveInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Handle handle = (Handle) context.getParameters()[0];
        handle.getEJBObject().remove();
        return null;
    }
}
