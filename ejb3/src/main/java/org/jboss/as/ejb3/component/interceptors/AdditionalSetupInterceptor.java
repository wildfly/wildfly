/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
        try {
            for (int i = 0; i < actions.length; ++i) {
                actions[i].setup(Collections.<String, Object>emptyMap());
            }
            return context.proceed();
        } finally {
            Throwable error = null;
            for (int i = actions.length - 1; i >=0; --i) {
                SetupAction action = actions[i];
                try {
                    action.teardown(Collections.<String, Object>emptyMap());
                } catch (Throwable e) {
                    error = e;
                }
            }
            if (error != null) {
                throw new RuntimeException(error);
            }
        }
    }

    public static InterceptorFactory factory(final List<SetupAction> actions) {
        final AdditionalSetupInterceptor interceptor = new AdditionalSetupInterceptor(actions);
        return new ImmediateInterceptorFactory(interceptor);
    }
}
