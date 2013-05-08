package org.jboss.as.weld.injection;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.weld.WeldMessages;
import org.jboss.weld.bean.ManagedBean;

/**
 * @author Stuart Douglas
 */
public class WeldInjectionContext implements Serializable {

    private final CreationalContext<?> context;
    private final Bean<?> bean;
    private final boolean delegateProduce;

    //the following fields are transient, as they are only needed at creation time,
    //and should not be needed after injection is complete
    private final transient InjectionTarget injectionTarget;
    private final transient Map<Class<?>, InjectionTarget> interceptorInjections;

    public WeldInjectionContext(CreationalContext<?> ctx, final Bean<?> bean, final boolean delegateProduce, final InjectionTarget injectionTarget, final Map<Class<?>, InjectionTarget> interceptorInjections) {
        this.context = ctx;
        this.bean = bean;
        this.delegateProduce = delegateProduce;
        this.injectionTarget = injectionTarget;
        this.interceptorInjections = interceptorInjections;
    }

    /**
     * Runs CDI injection on the instance. This should be called after resource injection has been performed
     */
    public void inject(Object instance) {
        injectionTarget.inject(instance, context);
    }

    public WeldManagedReference produce() {
        if (delegateProduce && bean instanceof ManagedBean) {
            final Object instance = ((ManagedBean) bean).getInjectionTarget().produce(context);
            return new WeldManagedReference(context, instance);
        } else {
            final Object instance = injectionTarget.produce(context);
            return new WeldManagedReference(context, instance);
        }
    }

    public void injectInterceptor(Object instance) {
        final InjectionTarget injection = interceptorInjections.get(instance.getClass());
        if (injection != null) {
            injection.inject(instance, context);
        } else {
            throw WeldMessages.MESSAGES.unknownInterceptorClassForCDIInjection(instance.getClass());
        }
    }

    public CreationalContext<?> getContext() {
        return context;
    }

    public InjectionTarget getInjectionTarget() {
        return injectionTarget;
    }

    public void release() {
        context.release();
    }

    private static final class WeldManagedReference implements ManagedReference, Serializable {
        private final CreationalContext<?> context;
        private final Object instance;

        private WeldManagedReference(final CreationalContext<?> context, final Object instance) {
            this.context = context;
            this.instance = instance;
        }

        @Override
        public void release() {
            context.release();
        }

        @Override
        public Object getInstance() {
            return instance;
        }
    }

}
