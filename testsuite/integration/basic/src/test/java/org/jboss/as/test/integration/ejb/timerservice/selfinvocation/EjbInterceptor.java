package org.jboss.as.test.integration.ejb.timerservice.selfinvocation;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class EjbInterceptor {

    public static boolean invoked = false;
    
    public EjbInterceptor() {
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        invoked = true;
        return ic.proceed();
    }
    
}
