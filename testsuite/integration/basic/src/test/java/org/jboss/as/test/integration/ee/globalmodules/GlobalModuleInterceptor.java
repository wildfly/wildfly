package org.jboss.as.test.integration.ee.globalmodules;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class GlobalModuleInterceptor {

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {

        return getClass().getSimpleName() + context.proceed();
    }

}
