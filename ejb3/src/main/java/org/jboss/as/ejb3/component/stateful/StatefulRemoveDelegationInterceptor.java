package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that delegates calls to EJB 2.x remove methods to the component remove method interceptor chain
 *
 * @author Stuart Douglas
 */
public class StatefulRemoveDelegationInterceptor  implements Interceptor{

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new StatefulRemoveDelegationInterceptor());

    private StatefulRemoveDelegationInterceptor() {

    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        StatefulSessionComponentInstance instance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
        return instance.getEjb2XRemoveInterceptor().processInvocation(context);
    }
}
