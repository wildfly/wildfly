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

package org.jboss.as.cmp.component;

import java.lang.reflect.Method;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanEjbCreateMethodInterceptorFactory;
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanRemoveInterceptorFactory;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.entity.EntityBeanObjectViewConfigurator;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author John Bailey
 */
public class CmpEntityBeanObjectViewConfigurator extends EntityBeanObjectViewConfigurator {
    protected InterceptorFactory getEjbCreateInterceptorFactory() {
        return CmpEntityBeanEjbCreateMethodInterceptorFactory.INSTANCE;
    }

    protected InterceptorFactory getEjbRemoveInterceptorFactory(final Method remove) {
        return new CmpEntityBeanRemoveInterceptorFactory(remove);
    }

    protected void handleNonBeanMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method) {
        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
        //TODO: these look like they are bypassing the synchronization interceptor
        configuration.addViewInterceptor(method, new ImmediateInterceptorFactory(new InstanceMethodInvokingInterceptor(method)), InterceptorOrder.View.COMPONENT_DISPATCHER);
    }

    private static class InstanceMethodInvokingInterceptor implements Interceptor {

        private final Method componentMethod;

        public InstanceMethodInvokingInterceptor(final Method componentMethod) {
            this.componentMethod = componentMethod;
        }

        public Object processInvocation(final InterceptorContext context) throws Exception {
            final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
            if (componentInstance == null) {
                throw CmpMessages.MESSAGES.noComponentInstanceAssociated();
            }
            //for CMP beans we invoke directly on the instance, bypassing the interceptor chain
            return componentMethod.invoke(componentInstance.getInstance(), context.getParameters());
        }
    }
}
