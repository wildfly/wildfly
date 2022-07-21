package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author Stuart Douglas
 */
public class AroundConstructInterceptorFactory implements InterceptorFactory {

    private final InterceptorFactory aroundConstrctChain;

    public AroundConstructInterceptorFactory(final InterceptorFactory aroundConstrctChain) {
        this.aroundConstrctChain = aroundConstrctChain;
    }


    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final Interceptor aroundConstruct = aroundConstrctChain.create(context);
        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                aroundConstruct.processInvocation(context);
                context.setParameters(null);
                return context.proceed();
            }
        };
    }
}
