package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 *
 */
@Interceptor
@SomeInterceptorBinding
public class SomeInterceptor {

    public static volatile int invocationCount;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        invocationCount++;
        /*System.out.println("invocationContext = " + invocationContext);
        System.out.println("invocationContext.getMethod() = " + invocationContext.getMethod());*/
        return invocationContext.proceed();
    }

}
