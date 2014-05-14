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
package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.webservices.injection.WSComponent;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Associates component instance for a POJO WS bean during invocation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSComponentInstanceAssociationInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new WSComponentInstanceAssociationInterceptor());

    private WSComponentInstanceAssociationInterceptor() {}

    @Override
    public Object processInvocation(final InterceptorContext interceptorContext) throws Exception {
        final WSComponent wsComponent = (WSComponent)interceptorContext.getPrivateData(Component.class);
        BasicComponentInstance pojoComponentInstance = null;
        if (interceptorContext.getPrivateData(ManagedReference.class) != null) {
           ManagedReference reference = interceptorContext.getPrivateData(ManagedReference.class);
           pojoComponentInstance = (BasicComponentInstance)wsComponent.createInstance(reference.getInstance());
        } else {
           pojoComponentInstance = wsComponent.getComponentInstance();
        }
        interceptorContext.putPrivateData(ComponentInstance.class, pojoComponentInstance);
        return interceptorContext.proceed();
    }
}
