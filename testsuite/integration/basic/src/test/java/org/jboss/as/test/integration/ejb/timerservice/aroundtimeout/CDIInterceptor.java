package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Interceptor
@Intercepted
public class CDIInterceptor {


    @AroundTimeout
    public Object aroundTimeout(final InvocationContext context) throws Exception {
        InterceptorOrder.intercept(CDIInterceptor.class);
        return context.proceed();
    }

}
