package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
