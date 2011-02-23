/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.ee.component;

import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.naming.JndiInjectable;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentInstallProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            // Nothing to do
            return;
        }
        final ModuleClassLoader classLoader = module.getClassLoader();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // Iterate through each component, installing it into the container
        for (AbstractComponentDescription description : moduleDescription.getComponentDescriptions()) {
            final String className = description.getComponentClassName();
            final Class<?> componentClass;
            try {
                componentClass = Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Component class not found", e);
            }
            final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(componentClass);
            final String applicationName = description.getApplicationName();
            final String moduleName = description.getModuleName();
            final String componentName = description.getComponentName();
            final ServiceName baseName = deploymentUnit.getServiceName().append("component").append(componentName);

            final AbstractComponentConfiguration configuration = description.createComponentConfiguration(phaseContext, componentClass);
            configuration.setComponentClass(componentClass);

            final ServiceName createServiceName = baseName.append("CREATE");
            final ServiceName startServiceName = baseName.append("START");
            final ComponentCreateService createService = new ComponentCreateService(configuration);
            final ServiceBuilder<Component> createBuilder = serviceTarget.addService(createServiceName, createService);
            final ComponentStartService startService = new ComponentStartService();
            final ServiceBuilder<Component> startBuilder = serviceTarget.addService(startServiceName, startService);

            // START depends on CREATE
            startBuilder.addDependency(createServiceName, Component.class, startService.getComponentInjector());

            // Iterate through each view, creating the services for each
            for (Map.Entry<Class<?>, ProxyFactory<?>> entry : configuration.getProxyFactories().entrySet()) {
                final Class<?> viewClass = entry.getKey();
                final ServiceName serviceName = baseName.append("VIEW").append(viewClass.getName());
                final ProxyFactory<?> proxyFactory = entry.getValue();
                final ViewService viewService = new ViewService(viewClass, proxyFactory);
                serviceTarget.addService(serviceName, viewService)
                        .addDependency(createServiceName, AbstractComponent.class, viewService.getComponentInjector())
                        .install();
            }

            // Iterate through each binding/injection, creating the JNDI binding and wiring dependencies for each
            final List<BindingDescription> bindingDescriptions = description.getBindings();
            final List<ResourceInjection> instanceResourceInjections = configuration.getResourceInjections();
            for (BindingDescription bindingDescription : bindingDescriptions) {
                addJndiDependency(classLoader, serviceTarget, description, componentClass, index, applicationName, moduleName, componentName, createServiceName, startBuilder, instanceResourceInjections, bindingDescription, phaseContext);
            }

            // Now iterate the interceptors and their bindings
            final Collection<InterceptorDescription> interceptorClasses = description.getAllInterceptors().values();

            for (InterceptorDescription interceptorDescription : interceptorClasses) {
                final List<ResourceInjection> interceptorResourceInjections = new ArrayList<ResourceInjection>();
                String interceptorClassName = interceptorDescription.getInterceptorClassName();
                final Class<?> interceptorClass;
                try {
                    interceptorClass = Class.forName(interceptorClassName, false, classLoader);
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Component interceptor class not found", e);
                }

                for (BindingDescription bindingDescription : interceptorDescription.getBindings()) {
                    addJndiDependency(classLoader, serviceTarget, description, interceptorClass, index, applicationName, moduleName, componentName, createServiceName, startBuilder, interceptorResourceInjections, bindingDescription, phaseContext);
                }
            }
            createBuilder.install();
            startBuilder.install();
        }
    }

    private static void addJndiDependency(final ModuleClassLoader classLoader, final ServiceTarget serviceTarget, final AbstractComponentDescription description, final Class<?> injecteeClass, final ClassReflectionIndex<?> index, final String applicationName, final String moduleName, final String componentName, final ServiceName createServiceName, final ServiceBuilder<Component> startBuilder, final List<ResourceInjection> instanceResourceInjections, final BindingDescription bindingDescription, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // Gather information about the dependency
        final String bindingName = bindingDescription.getBindingName();
        final String bindingType = bindingDescription.getBindingType();
        try {
            // Sanity check before we invest a lot of effort
            Class.forName(bindingType, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Component binding class not found", e);
        }

        // There are four applicable scenarios for an injectable dependency
        // 1. The source is a service, and the resource is bound into JNDI (start->binding->service)
        // 2. The source is a service but the resource is not bound into JNDI (start->service)
        // 3. The source is not a service and the resource is bound into JNDI (start->binding)
        // 4. The source is not a service and the resource is not bound (no dependency)

        Value<JndiInjectable> resourceValue;

        // Check to see if this entry should actually be bound into JNDI.
        if (bindingName != null) {
            // bind into JNDI
            // The binding depends on the bound resource, the creation of this component, and the scope naming store.
            ComponentNamingMode namingMode = description.getNamingMode();
            final String fullBindingName;
            final String serviceBindingName;
            if (bindingDescription.isAbsoluteBinding()) {
                fullBindingName = bindingName;
                int idx = fullBindingName.indexOf('/');
                if (idx == -1) {
                    serviceBindingName = fullBindingName;
                } else {
                    serviceBindingName = fullBindingName.substring(idx + 1);
                }
            } else if (namingMode == ComponentNamingMode.CREATE) {
                fullBindingName = "java:comp/env/" + bindingName;
                serviceBindingName = "env/" + bindingName;
            } else {
                fullBindingName = "java:module/env/" + bindingName;
                serviceBindingName = "env/" + bindingName;
            }
            final BinderService service = new BinderService(serviceBindingName);
            final ServiceName bindingServiceName = ContextNames.serviceNameOfContext(applicationName, moduleName, componentName, fullBindingName);
            if (bindingServiceName == null) {
                throw new IllegalArgumentException("Invalid context name '" + bindingName + "' for binding");
            }
            // The service builder for the binding
            ServiceBuilder<JndiInjectable> sourceServiceBuilder = serviceTarget.addService(bindingServiceName, service);
            // The resource value is determined by the reference source, which may add a dependency on the original value to the binding
            bindingDescription.getReferenceSourceDescription().getResourceValue(description, bindingDescription, sourceServiceBuilder, phaseContext, service.getJndiInjectableInjector());
            resourceValue = sourceServiceBuilder
                    .addDependency(createServiceName)
                    .addDependency(bindingServiceName.getParent(), NamingStore.class, service.getNamingStoreInjector())
                    .install();
            // Start service depends on the binding, one way or another
            // Note that view bindings will also be added as a dependency of the START phase; not strictly necessary but not harmful either
            startBuilder.addDependency(bindingServiceName);
        } else {
            // do not bind into JNDI
            // The resource value comes from the reference source, which may add a dependency on the original value to the start service
            final InjectedValue<JndiInjectable> injectedValue = new InjectedValue<JndiInjectable>();
            bindingDescription.getReferenceSourceDescription().getResourceValue(description, bindingDescription, startBuilder, phaseContext, injectedValue);
            resourceValue = injectedValue;
        }
        // Create injectors for the binding
        for (InjectionTargetDescription targetDescription : bindingDescription.getInjectionTargetDescriptions()) {
            instanceResourceInjections.add(ResourceInjection.Factory.create(targetDescription, injecteeClass, index, resourceValue));
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
