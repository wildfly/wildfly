package org.jboss.as.ee.component.interceptors;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
* @author Stuart Douglas
*/
public class ComponentDispatcherInterceptor implements Interceptor {

    private final Method componentMethod;

    public ComponentDispatcherInterceptor(final Method componentMethod) {
        this.componentMethod = componentMethod;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        if (componentInstance == null) {
            throw new IllegalStateException("No component instance associated");
        }
        Method oldMethod = context.getMethod();
        try {
            context.setMethod(componentMethod);
            context.setTarget(componentInstance.getInstance());
            return componentInstance.getInterceptor(componentMethod).processInvocation(context);
        } finally {
            context.setMethod(oldMethod);
            context.setTarget(null);
        }
    }
}
