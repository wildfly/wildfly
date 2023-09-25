/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.interceptors;

import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that performs additional setup for remote and timer invocations (i.e invocations that are performed
 * from outside an existing EE context).
 *
 * @author Stuart Douglas
 */
public class AdditionalSetupInterceptor implements Interceptor {

    private final SetupAction[] actions;

    public AdditionalSetupInterceptor(final List<SetupAction> actions) {
        this.actions = actions.toArray(new SetupAction[actions.size()]);
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        Throwable originalException = null;
        Object retValue = null;
        try {
            for (int i = 0; i < actions.length; ++i) {
                actions[i].setup(Collections.<String, Object>emptyMap());
            }
            retValue = context.proceed();
        } catch(Throwable ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            originalException = ex;
        } finally {
            Throwable suppressed = originalException;
            for (int i = actions.length - 1; i >=0; --i) {
                SetupAction action = actions[i];
                try {
                    action.teardown(Collections.<String, Object>emptyMap());
                } catch (Throwable t) {
                    if (suppressed != null) {
                        suppressed.addSuppressed(t);
                    } else {
                        suppressed = t instanceof RuntimeException ? t : new RuntimeException(t);
                    }
                }
            }
            if (suppressed != null) {
                if (suppressed instanceof RuntimeException) {
                    throw (RuntimeException) suppressed;
                }
                if (suppressed instanceof Exception) {
                    throw (Exception) suppressed;
                }
                if (suppressed instanceof Error) {
                    throw (Error) suppressed;
                }
            }
        }
        return retValue;
    }

    public static InterceptorFactory factory(final List<SetupAction> actions) {
        final AdditionalSetupInterceptor interceptor = new AdditionalSetupInterceptor(actions);
        return new ImmediateInterceptorFactory(interceptor);
    }
}
