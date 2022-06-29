package org.jboss.as.test.integration.ejb.remote.contextdata;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class EjbInterceptor {


    @AroundInvoke
    public Object interceptor(InvocationContext context) throws Exception {
        context.getContextData().put("data1", "client interceptor data(" + context.getContextData().get("clientData") + ")");
        return context.proceed();
    }

}
