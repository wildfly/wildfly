package org.jboss.as.weld.deployment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.EEClassIntrospector;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.injection.InjectionTargets;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.as.weld.util.Utils;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldClassIntrospector implements EEClassIntrospector, Service {

    private static final ServiceName SERVICE_NAME = ServiceName.of("weld", "weldClassIntrospector");
    private final Consumer<EEClassIntrospector> eeClassIntrospectorConsumer;
    private final Supplier<BeanManager> beanManagerSupplier;
    private final ConcurrentMap<Class<?>, InjectionTarget<?>> injectionTargets = new ConcurrentHashMap<>();

    private WeldClassIntrospector(final Consumer<EEClassIntrospector> eeClassIntrospectorConsumer, final Supplier<BeanManager> beanManagerSupplier) {
        this.eeClassIntrospectorConsumer = eeClassIntrospectorConsumer;
        this.beanManagerSupplier = beanManagerSupplier;
    }

    public static void install(final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget) {
        final ServiceName sn = serviceName(deploymentUnit);
        final ServiceBuilder<?> sb = serviceTarget.addService(sn);
        final Consumer<EEClassIntrospector> eeClassIntrospectorConsumer = sb.provides(sn);
        final Supplier<BeanManager> beanManagerSupplier = sb.requires(BeanManagerService.serviceName(deploymentUnit));
        sb.requires(Utils.getRootDeploymentUnit(deploymentUnit).getServiceName().append(WeldStartService.SERVICE_NAME));
        sb.setInstance(new WeldClassIntrospector(eeClassIntrospectorConsumer, beanManagerSupplier));
        sb.install();
    }

    public static ServiceName serviceName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(SERVICE_NAME);
    }

    @Override
    public ManagedReferenceFactory createFactory(Class<?> clazz) {

        final BeanManager beanManager = this.beanManagerSupplier.get();
        final InjectionTarget injectionTarget = getInjectionTarget(clazz);
        return new ManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                final CreationalContext context = beanManager.createCreationalContext(null);

                Object instance;
                BasicInjectionTarget target = injectionTarget instanceof BasicInjectionTarget ? (BasicInjectionTarget) injectionTarget: null;
                if(target != null && target.getBean() != null) {
                    instance = beanManager.getReference(target.getBean(), target.getAnnotatedType().getBaseType(), context);
                } else {
                    instance = injectionTarget.produce(context);
                }

                injectionTarget.inject(instance, context);
                injectionTarget.postConstruct(instance);
                return new WeldManagedReference(injectionTarget, context, instance);
            }
        };
    }

    private InjectionTarget getInjectionTarget(Class<?> clazz) {
        InjectionTarget<?> target = injectionTargets.get(clazz);
        if (target != null) {
            return target;
        }
        final BeanManagerImpl beanManager = BeanManagerProxy.unwrap(beanManagerSupplier.get());
        Bean<?> bean = null;
        Set<Bean<?>> beans = new HashSet<>(beanManager.getBeans(clazz, Any.Literal.INSTANCE));
        Iterator<Bean<?>> it = beans.iterator();
        //we may have resolved some sub-classes
        //go through and remove them from the bean set
        while (it.hasNext()) {
            Bean<?> b = it.next();
            if(b.getBeanClass() != clazz) {
                it.remove();
            }
        }
        if(beans.size() == 1) {
            bean = beans.iterator().next();
        }
        InjectionTarget<?> newTarget = InjectionTargets.createInjectionTarget(clazz, bean, beanManager, true);
        target = injectionTargets.putIfAbsent(clazz, newTarget);
        if (target == null) {
            return newTarget;
        } else {
            return target;
        }
    }

    @Override
    public ManagedReference createInstance(final Object instance) {
        return this.getReference(instance, true);
    }

    @Override
    public ManagedReference getInstance(Object instance) {
        return this.getReference(instance, false);
    }

    private ManagedReference getReference(Object instance, boolean newInstance) {
        final BeanManager beanManager = beanManagerSupplier.get();
        final InjectionTarget injectionTarget = getInjectionTarget(instance.getClass());
        final CreationalContext context = beanManager.createCreationalContext(null);
        if (newInstance) {
            injectionTarget.inject(instance, context);
            injectionTarget.postConstruct(instance);
        }
        return new WeldManagedReference(injectionTarget, context, instance);
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        eeClassIntrospectorConsumer.accept(this);
    }

    @Override
    public void stop(StopContext stopContext) {
        injectionTargets.clear();
    }

    private static class WeldManagedReference implements ManagedReference {

        private final InjectionTarget injectionTarget;
        private final CreationalContext ctx;
        private final Object instance;

        public WeldManagedReference(InjectionTarget injectionTarget, CreationalContext ctx, Object instance) {
            this.injectionTarget = injectionTarget;
            this.ctx = ctx;
            this.instance = instance;
        }

        @Override
        public void release() {
            try {
                injectionTarget.preDestroy(instance);
            } finally {
                ctx.release();
            }
        }

        @Override
        public Object getInstance() {
            return instance;
        }
    }
}
