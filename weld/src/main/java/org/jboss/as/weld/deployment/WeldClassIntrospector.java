package org.jboss.as.weld.deployment;

import org.jboss.as.ee.component.EEClassIntrospector;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Stuart Douglas
 */
public class WeldClassIntrospector implements EEClassIntrospector, Service<EEClassIntrospector> {

    private static final ServiceName SERVICE_NAME = ServiceName.of("weld", "weldClassIntrospector");

    private final InjectedValue<BeanManager> beanManager = new InjectedValue<>();

    private final ConcurrentMap<Class<?>, InjectionTarget<?>> injectionTargets = new ConcurrentHashMap<>();

    public static void install(final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget) {
        final WeldClassIntrospector introspector = new WeldClassIntrospector();
        serviceTarget.addService(serviceName(deploymentUnit), introspector)
                .addDependency(BeanManagerService.serviceName(deploymentUnit), BeanManager.class, introspector.beanManager)
                .install();
    }

    public static ServiceName serviceName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(SERVICE_NAME);
    }

    @Override
    public ManagedReferenceFactory createFactory(Class<?> clazz) {

        final BeanManager beanManager = this.beanManager.getValue();
        final InjectionTarget injectionTarget = getInjectionTarget(clazz);
        return new ManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                final CreationalContext context = beanManager.createCreationalContext(null);
                final Object instance = injectionTarget.produce(context);
                injectionTarget.inject(instance, context);
                injectionTarget.postConstruct(instance);
                return new ManagedReference() {
                    @Override
                    public void release() {
                        context.release();
                    }

                    @Override
                    public Object getInstance() {
                        return instance;
                    }
                };
            }
        };
    }

    private InjectionTarget getInjectionTarget(Class<?> clazz) {
        InjectionTarget<?> target = injectionTargets.get(clazz);
        if (target != null) {
            return target;
        }
        final BeanManager beanManager = this.beanManager.getValue();
        InjectionTarget<?> newTarget = beanManager.createInjectionTarget(beanManager.createAnnotatedType(clazz));
        target = injectionTargets.putIfAbsent(clazz, newTarget);
        if (target == null) {
            return newTarget;
        } else {
            return target;
        }
    }

    @Override
    public ManagedReference createInstance(final Object instance) {
        final BeanManager beanManager = this.beanManager.getValue();
        final InjectionTarget injectionTarget = getInjectionTarget(instance.getClass());
        final CreationalContext context = beanManager.createCreationalContext(null);
        injectionTarget.inject(instance, context);
        injectionTarget.postConstruct(instance);
        return new ManagedReference() {
            @Override
            public void release() {
                context.release();
            }

            @Override
            public Object getInstance() {
                return instance;
            }
        };
    }

    public InjectedValue<BeanManager> getBeanManager() {
        return beanManager;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
        injectionTargets.clear();
    }

    @Override
    public EEClassIntrospector getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
