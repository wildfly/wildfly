package org.jboss.as.test.integration.ejb.timerservice.selfinvocation;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@CdiIntercepted
@Interceptor
public class CdiInterceptor {

    public static boolean invoked = false;
    
    public CdiInterceptor() {
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        invoked = true;
        return ic.proceed();
    }

}
