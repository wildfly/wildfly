package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Intercepted
@Interceptor
public class StatefulCDIInterceptor implements Serializable {

    private final AtomicInteger count = new AtomicInteger(0);

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        return ((Integer)context.proceed() ) + count.addAndGet(10000);
    }

}
