package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
