package org.jboss.as.weld.deployment;

import org.jboss.as.ee.component.EEClassIntrospector;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * @author Stuart Douglas
 */
public class WeldClassIntrospector implements EEClassIntrospector {

    private static final ServiceName SERVICE_NAME = ServiceName.of("weld", "weldClassIntrospector");

    private final InjectedValue<BeanManager> beanManager = new InjectedValue<>();

    public static void install(final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget) {
        final WeldClassIntrospector introspector = new WeldClassIntrospector();
        serviceTarget.addService(serviceName(deploymentUnit), new ValueService<Object>(new ImmediateValue<Object>(introspector)))
                .addDependency(BeanManagerService.serviceName(deploymentUnit), BeanManager.class, introspector.beanManager)
                .install();
    }

    public static ServiceName serviceName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(SERVICE_NAME);
    }

    @Override
    public ManagedReferenceFactory createFactory(Class<?> clazz) {

        final BeanManager beanManager = this.beanManager.getValue();
        final InjectionTarget injectionTarget = beanManager.createInjectionTarget(beanManager.createAnnotatedType(clazz));
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

    public InjectedValue<BeanManager> getBeanManager() {
        return beanManager;
    }
}
