package org.jboss.as.weld.injection;

import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.weld.construction.api.AroundConstructCallback;
import org.jboss.weld.construction.api.ConstructionHandle;
import org.jboss.weld.construction.api.WeldCreationalContext;

/**
 * Initiates component construction. This interceptor delegates to Weld to start component construction. When Weld resolves the values of constructor
 * parameters, it invokes a callback which allows WF to perform AroundConstruct interception. The callback is registered within
 * {@link #setupAroundConstructCallback(CreationalContext, InterceptorContext)}
 *
 * @author Jozef Hartinger
 *
 */
public class WeldConstructionStartInterceptor implements Interceptor {

    public static final WeldConstructionStartInterceptor INSTANCE = new WeldConstructionStartInterceptor();

    private WeldConstructionStartInterceptor() {
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        WeldInjectionContext injectionCtx = context.getPrivateData(WeldInjectionContext.class);
        setupAroundConstructCallback(injectionCtx.getContext(), context);
        /*
         * Here we delegate to Weld to start component construction. WF will be called back on the callback registered within #setupAroundConstructCallback.
         * The return value of the following call is not important as we get the component reference from ConstructionContext in WeldManagedReferenceFactory.
         */
        injectionCtx.produce();
        return context.getTarget();
    }

    private <T> void setupAroundConstructCallback(CreationalContext<T> ctx, final InterceptorContext context) {
        WeldCreationalContext<T> ctxImpl = (WeldCreationalContext<T>) ctx;
        ctxImpl.setConstructorInterceptionSuppressed(true); // Weld will not try to invoke around construct interceptors on this instance

        ctxImpl.registerAroundConstructCallback(new AroundConstructCallback<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T aroundConstruct(ConstructionHandle<T> ctx, AnnotatedConstructor<T> constructor, Object[] parameters, Map<String, Object> data) throws Exception {
                context.putPrivateData(ConstructionHandle.class, ctx);
                context.setParameters(parameters);
                context.setContextData(data);
                context.setConstructor(constructor.getJavaMember());
                context.proceed(); // proceed with the WF interceptor chain
                return (T) context.getTarget();
            }
        });
    }

}
