package org.jboss.as.ejb3.component.entity.interceptors;

import org.jboss.as.ejb3.timerservice.TimerServiceDisabledTacker;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author Stuart Douglas
 */
public class DisableTimerServiceInterceptorFactory implements InterceptorFactory {

    private final Interceptor interceptor;

    public DisableTimerServiceInterceptorFactory(final String methodName) {
        interceptor = new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                String prev = TimerServiceDisabledTacker.getDisabledReason();
                try {
                    TimerServiceDisabledTacker.setDisabledReason(methodName);
                    return context.proceed();
                } finally {
                    TimerServiceDisabledTacker.setDisabledReason(prev);
                }
            }
        };
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        return interceptor;
    }
}
