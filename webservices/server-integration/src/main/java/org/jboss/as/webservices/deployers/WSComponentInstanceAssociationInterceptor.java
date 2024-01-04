/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
