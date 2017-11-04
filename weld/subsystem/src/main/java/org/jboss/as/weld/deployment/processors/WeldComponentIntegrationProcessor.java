/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.interceptors.Jsr299BindingsInterceptor.factory;
import static org.jboss.as.weld.util.Utils.getRootDeploymentUnit;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.InterceptionType;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.interceptors.UserInterceptorFactory;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.deployment.WeldClassIntrospector;
import org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor;
import org.jboss.as.weld.ejb.WeldInterceptorBindingsService;
import org.jboss.as.weld.injection.WeldComponentService;
import org.jboss.as.weld.injection.WeldConstructionStartInterceptor;
import org.jboss.as.weld.injection.WeldInjectionContextInterceptor;
import org.jboss.as.weld.injection.WeldInjectionInterceptor;
import org.jboss.as.weld.injection.WeldInterceptorInjectionInterceptor;
import org.jboss.as.weld.injection.WeldManagedReferenceFactory;
import org.jboss.as.weld.interceptors.Jsr299BindingsCreateInterceptor;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.spi.ComponentIntegrator;
import org.jboss.as.weld.spi.ComponentIntegrator.DefaultInterceptorIntegrationAction;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.as.weld.util.ServiceLoaders;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment unit processor that add the {@link org.jboss.as.weld.injection.WeldManagedReferenceFactory} instantiator
 * to components that are part of a bean archive.
 *
 * @author Stuart Douglas
 */
public class WeldComponentIntegrationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            return;
        }

        final DeploymentUnit topLevelDeployment = getRootDeploymentUnit(deploymentUnit);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ServiceName weldBootstrapService = topLevelDeployment.getServiceName().append(WeldBootstrapService.SERVICE_NAME);
        final ServiceName weldStartService = topLevelDeployment.getServiceName().append(WeldStartService.SERVICE_NAME);
        final ServiceName beanManagerService = ServiceNames.beanManagerServiceName(deploymentUnit);

        final Iterable<ComponentIntegrator> componentIntegrators = ServiceLoader.load(ComponentIntegrator.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldComponentIntegrationProcessor.class));
        final ComponentInterceptorSupport componentInterceptorSupport = ServiceLoaders.loadSingle(ComponentInterceptorSupport.class,
                WeldComponentIntegrationProcessor.class).orElse(null);

        WeldClassIntrospector.install(deploymentUnit, phaseContext.getServiceTarget());

        eeModuleDescription.setDefaultClassIntrospectorServiceName(WeldClassIntrospector.serviceName(deploymentUnit));

        for (ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            final String beanName;
            if (isBeanNameRequired(component, componentIntegrators)) {
                beanName = component.getComponentName();
            } else {
                beanName = null;
            }
            component.getConfigurators().add((context, description, configuration) -> {

                //add interceptor to activate the request scope if required
                final EjbRequestScopeActivationInterceptor.Factory requestFactory = new EjbRequestScopeActivationInterceptor.Factory(beanManagerService);

                for(ViewConfiguration view : configuration.getViews()) {
                    view.addViewInterceptor(requestFactory, InterceptorOrder.View.CDI_REQUEST_SCOPE);
                }
                configuration.addTimeoutViewInterceptor(requestFactory, InterceptorOrder.View.CDI_REQUEST_SCOPE);
            });
            component.getConfigurators().addFirst(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final Class<?> componentClass = configuration.getComponentClass();
                    final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
                    final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                    final ModuleClassLoader classLoader = module.getClassLoader();

                    //get the interceptors so they can be injected as well
                    final Set<Class<?>> interceptorClasses = new HashSet<Class<?>>();
                    for (InterceptorDescription interceptorDescription : description.getAllInterceptors()) {
                        try {
                            interceptorClasses.add(ClassLoadingUtils.loadClass(interceptorDescription.getInterceptorClassName(), module));
                        } catch (ClassNotFoundException e) {
                            throw WeldLogger.ROOT_LOGGER.couldNotLoadInterceptorClass(interceptorDescription.getInterceptorClassName(), e);
                        }
                    }
                    addWeldIntegration(componentIntegrators, componentInterceptorSupport, context.getServiceTarget(), configuration, description,
                            componentClass, beanName, weldBootstrapService, weldStartService, beanManagerService,
                            interceptorClasses, classLoader, description.getBeanDeploymentArchiveId());
                }
            });
        }

    }

    private boolean isBeanNameRequired(ComponentDescription component, Iterable<ComponentIntegrator> integrators) {
        for (ComponentIntegrator integrator : integrators) {
            if (integrator.isBeanNameRequired(component)) {
                return true;
            }
        }
        return false;
    }

    private boolean isComponentWithView(ComponentDescription component, Iterable<ComponentIntegrator> integrators) {
        for (ComponentIntegrator integrator : integrators) {
            if (integrator.isComponentWithView(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * As the weld based instantiator needs access to the bean manager it is installed as a service.
     */
    private void addWeldIntegration(final Iterable<ComponentIntegrator> componentIntegrators, final ComponentInterceptorSupport componentInterceptorSupport, final ServiceTarget target, final ComponentConfiguration configuration, final ComponentDescription description, final Class<?> componentClass, final String beanName, final ServiceName weldServiceName, final ServiceName weldStartService, final ServiceName beanManagerService, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId) {

        final ServiceName serviceName = configuration.getComponentDescription().getServiceName().append("WeldInstantiator");

        final WeldComponentService weldComponentService = new WeldComponentService(componentClass, beanName, interceptorClasses, classLoader,
                beanDeploymentArchiveId, description.isCDIInterceptorEnabled(), description, isComponentWithView(description, componentIntegrators));
        final ServiceBuilder<WeldComponentService> builder = target.addService(serviceName, weldComponentService)
                .addDependency(weldServiceName, WeldBootstrapService.class, weldComponentService.getWeldContainer()).addDependency(weldStartService);

        configuration.setInstanceFactory(WeldManagedReferenceFactory.INSTANCE);
        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            @Override
            public void configureDependency(final ServiceBuilder<?> serviceBuilder, ComponentStartService service) throws DeploymentUnitProcessingException {
                serviceBuilder.addDependency(serviceName);
            }
        });

        boolean isComponentIntegrationPerformed = false;
        for (ComponentIntegrator componentIntegrator : componentIntegrators) {
            Supplier<ServiceName> bindingServiceNameSupplier = () -> {
                if (componentInterceptorSupport == null) {
                    throw WeldLogger.DEPLOYMENT_LOGGER.componentInterceptorSupportNotAvailable(componentClass);
                }
                return addWeldInterceptorBindingService(target, configuration, componentClass, beanName, weldServiceName, weldStartService,
                        beanDeploymentArchiveId, componentInterceptorSupport);
            };
            DefaultInterceptorIntegrationAction integrationAction = (bindingServiceName) -> {
                if (componentInterceptorSupport == null) {
                    throw WeldLogger.DEPLOYMENT_LOGGER.componentInterceptorSupportNotAvailable(componentClass);
                }
                addJsr299BindingsCreateInterceptor(configuration, description, beanName, weldServiceName, builder, bindingServiceName,
                        componentInterceptorSupport);
                addCommonLifecycleInterceptionSupport(configuration, builder, bindingServiceName, beanManagerService, componentInterceptorSupport);
                configuration.addComponentInterceptor(
                        new UserInterceptorFactory(factory(InterceptionType.AROUND_INVOKE, builder, bindingServiceName, componentInterceptorSupport),
                                factory(InterceptionType.AROUND_TIMEOUT, builder, bindingServiceName, componentInterceptorSupport)),
                        InterceptorOrder.Component.CDI_INTERCEPTORS, false);
            };
            if (componentIntegrator.integrate(beanManagerService, configuration, description, builder, bindingServiceNameSupplier, integrationAction,
                    componentInterceptorSupport)) {
                isComponentIntegrationPerformed = true;
                break;
            }
        }

        if (!isComponentIntegrationPerformed) {
            description.setIgnoreLifecycleInterceptors(true); //otherwise they will be called twice
            // for components with no view register interceptors that delegate to InjectionTarget lifecycle methods to trigger lifecycle interception
            configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(new AbstractInjectionTargetDelegatingInterceptor() {
                @Override
                protected void run(Object instance) {
                    weldComponentService.getInjectionTarget().postConstruct(instance);
                }
            }), InterceptorOrder.ComponentPostConstruct.CDI_INTERCEPTORS);
            configuration.addPreDestroyInterceptor(new ImmediateInterceptorFactory(new AbstractInjectionTargetDelegatingInterceptor() {
                @Override
                protected void run(Object instance) {
                    weldComponentService.getInjectionTarget().preDestroy(instance);
                }
            }), InterceptorOrder.ComponentPreDestroy.CDI_INTERCEPTORS);
        }

        builder.install();

        configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(new WeldInjectionContextInterceptor(weldComponentService)), InterceptorOrder.ComponentPostConstruct.WELD_INJECTION_CONTEXT_INTERCEPTOR);
        configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(new WeldInterceptorInjectionInterceptor(interceptorClasses)), InterceptorOrder.ComponentPostConstruct.INTERCEPTOR_WELD_INJECTION);
        configuration.addPostConstructInterceptor(WeldInjectionInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.COMPONENT_WELD_INJECTION);

    }

    private static ServiceName addWeldInterceptorBindingService(final ServiceTarget target, final ComponentConfiguration configuration, final Class<?> componentClass, final String beanName, final ServiceName weldServiceName, final ServiceName weldStartService, final String beanDeploymentArchiveId, final ComponentInterceptorSupport componentInterceptorSupport) {
        final WeldInterceptorBindingsService weldInterceptorBindingsService = new WeldInterceptorBindingsService(beanDeploymentArchiveId, beanName, componentClass, componentInterceptorSupport);
        ServiceName bindingServiceName = configuration.getComponentDescription().getServiceName().append(WeldInterceptorBindingsService.SERVICE_NAME);
        target.addService(bindingServiceName, weldInterceptorBindingsService)
                .addDependency(weldServiceName, WeldBootstrapService.class, weldInterceptorBindingsService.getWeldContainer())
                .addDependency(weldStartService)
                .install();
        return bindingServiceName;
    }

    private static void addJsr299BindingsCreateInterceptor(final ComponentConfiguration configuration, final ComponentDescription description, final String beanName, final ServiceName weldServiceName, ServiceBuilder<WeldComponentService> builder, final ServiceName bindingServiceName, final ComponentInterceptorSupport componentInterceptorSupport) {
        //add the create interceptor that creates the CDI interceptors
        final Jsr299BindingsCreateInterceptor createInterceptor = new Jsr299BindingsCreateInterceptor(description.getBeanDeploymentArchiveId(), beanName, componentInterceptorSupport);
        configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(createInterceptor), InterceptorOrder.ComponentPostConstruct.CREATE_CDI_INTERCEPTORS);
        builder.addDependency(weldServiceName, WeldBootstrapService.class, createInterceptor.getWeldContainer());
        builder.addDependency(bindingServiceName, InterceptorBindings.class, createInterceptor.getInterceptorBindings());
    }

    private static void addCommonLifecycleInterceptionSupport(final ComponentConfiguration configuration, ServiceBuilder<WeldComponentService> builder, final ServiceName bindingServiceName, final ServiceName beanManagerServiceName, final ComponentInterceptorSupport componentInterceptorSupport) {
        configuration.addPreDestroyInterceptor(factory(InterceptionType.PRE_DESTROY, builder, bindingServiceName, componentInterceptorSupport), InterceptorOrder.ComponentPreDestroy.CDI_INTERCEPTORS);
        configuration.addAroundConstructInterceptor(factory(InterceptionType.AROUND_CONSTRUCT, builder, bindingServiceName, componentInterceptorSupport), InterceptorOrder.AroundConstruct.WELD_AROUND_CONSTRUCT_INTERCEPTORS);
        configuration.addPostConstructInterceptor(factory(InterceptionType.POST_CONSTRUCT, builder, bindingServiceName, componentInterceptorSupport), InterceptorOrder.ComponentPostConstruct.CDI_INTERCEPTORS);
        /*
         * Add interceptor to activate the request scope for the @PostConstruct callback.
         * See https://issues.jboss.org/browse/CDI-219 for details
         */
        final EjbRequestScopeActivationInterceptor.Factory postConstructRequestContextActivationFactory = new EjbRequestScopeActivationInterceptor.Factory(beanManagerServiceName);
        configuration.addPostConstructInterceptor(postConstructRequestContextActivationFactory, InterceptorOrder.ComponentPostConstruct.REQUEST_SCOPE_ACTIVATING_INTERCEPTOR);
        // @AroundConstruct support
        configuration.addAroundConstructInterceptor(new ImmediateInterceptorFactory(WeldConstructionStartInterceptor.INSTANCE), InterceptorOrder.AroundConstruct.CONSTRUCTION_START_INTERCEPTOR);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Retrieves ManagedReference from the interceptor context and performs an InjectionTarget operation on the instance
     */
    private abstract static class AbstractInjectionTargetDelegatingInterceptor implements Interceptor {

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            ManagedReference reference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(BasicComponentInstance.INSTANCE_KEY);
            if (reference != null) {
                run(reference.getInstance());
            }
            return context.proceed();
        }

        protected abstract void run(Object instance);
    }

}
