package org.jboss.as.ejb3.component.interceptors;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that performs additional setup for remote and timer invocations (i.e invocations that are performed
 * from outside an existing EE context).
 *
 * @author Stuart Douglas
 */
public class AdditionalSetupInterceptor implements Interceptor {

    private final List<SetupAction> actions;

    public AdditionalSetupInterceptor(final List<SetupAction> actions) {
        this.actions = actions;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final InvocationType invocationType = context.getPrivateData(InvocationType.class);
        if(invocationType == null) {
            return context.proceed();
        }
        switch (invocationType) {
            case TIMER:
            case ASYNC:
            case REMOTE:
            case MESSAGE_DELIVERY:
                try {
                    for (SetupAction action : actions) {
                        action.setup(Collections.<String, Object>emptyMap());
                    }
                    return context.proceed();
                } finally {
                    final ListIterator<SetupAction> iterator = actions.listIterator(actions.size());
                    Throwable error = null;
                    while (iterator.hasPrevious()) {
                        SetupAction action = iterator.previous();
                        try {
                            action.teardown(Collections.<String, Object>emptyMap());
                        } catch (Throwable e) {
                            error = e;
                        }
                    }
                    if (error != null) {
                        throw new RuntimeException(error);
                    }
                }
            default:
                return context.proceed();
        }
    }

    public static InterceptorFactory factory(final List<SetupAction> actions) {
        final AdditionalSetupInterceptor interceptor = new AdditionalSetupInterceptor(actions);
        return new ImmediateInterceptorFactory(interceptor);
    }
}
