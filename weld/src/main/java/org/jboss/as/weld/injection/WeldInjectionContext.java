package org.jboss.as.weld.injection;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.weld.bean.ManagedBean;

/**
 * @author Stuart Douglas
 */
class WeldInjectionContext implements Serializable {

    private final CreationalContext<?> context;
    private final Bean<?> bean;
    private final boolean delegateProduce;

    //the following fields are transient, as they are only needed at creation time,
    //and should not be needed after injection is complete
    private final transient InjectionTarget injectionTarget;
    private final transient Map<Class<?>, InjectionTarget> interceptorInjections;

    WeldInjectionContext(CreationalContext<?> ctx, final Bean<?> bean, final boolean delegateProduce, final InjectionTarget injectionTarget, final Map<Class<?>, InjectionTarget> interceptorInjections) {
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

    public Object produce() {
        if (delegateProduce && bean instanceof ManagedBean) {
            return ((ManagedBean) bean).getInjectionTarget().produce(context);
        } else {
            return injectionTarget.produce(context);
        }
    }

    public void injectInterceptor(Object instance) {
        final InjectionTarget injection = interceptorInjections.get(instance.getClass());
        if (injection != null) {
            injection.inject(instance, context);
        } else {
            throw WeldLogger.ROOT_LOGGER.unknownInterceptorClassForCDIInjection(instance.getClass());
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
}
