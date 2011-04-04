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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * A description of a generic Java EE component.  The description is pre-classloading so it references everything by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ComponentDescription extends LifecycleCapableDescription {

    private static final DefaultFirstConfigurator FIRST_CONFIGURATOR = new DefaultFirstConfigurator();

    private final ServiceName serviceName;
    private final String componentName;
    private final String componentClassName;
    private final EEModuleDescription moduleDescription;
    private final EEModuleClassDescription classDescription;

    private final List<InterceptorDescription> classInterceptors = new ArrayList<InterceptorDescription>();
    private final Set<String> classInterceptorsSet = new HashSet<String>();

    private final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = new HashMap<MethodIdentifier, List<InterceptorDescription>>();
    private final Map<MethodIdentifier, Set<String>> methodInterceptorsSet = new HashMap<MethodIdentifier, Set<String>>();

    private final Set<MethodIdentifier> methodExcludeDefaultInterceptors = new HashSet<MethodIdentifier>();
    private final Set<MethodIdentifier> methodExcludeClassInterceptors = new HashSet<MethodIdentifier>();

    private final Map<String, InterceptorDescription> allInterceptors = new HashMap<String, InterceptorDescription>();

    private final Map<ServiceName, ServiceBuilder.DependencyType> dependencies = new HashMap<ServiceName, ServiceBuilder.DependencyType>();


    private ComponentNamingMode namingMode = ComponentNamingMode.NONE;
    private boolean excludeDefaultInterceptors = false;
    private DeploymentDescriptorEnvironment deploymentDescriptorEnvironment;

    private final List<ViewDescription> views = new ArrayList<ViewDescription>();

    private final Deque<ComponentConfigurator> configurators = new ArrayDeque<ComponentConfigurator>();

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param moduleDescription  the EE module description
     * @param classDescription   the component class' description
     * @param deploymentUnitServiceName the service name of the DU containing this component
     */
    public ComponentDescription(final String componentName, final String componentClassName, final EEModuleDescription moduleDescription, final EEModuleClassDescription classDescription, final ServiceName deploymentUnitServiceName) {
        this.moduleDescription = moduleDescription;
        this.classDescription = classDescription;
        serviceName = deploymentUnitServiceName.append("component");
        if (componentName == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (componentClassName == null) {
            throw new IllegalArgumentException("className is null");
        }
        if (moduleDescription == null) {
            throw new IllegalArgumentException("moduleName is null");
        }
        if (classDescription == null) {
            throw new IllegalArgumentException("classDescription is null");
        }
        if (deploymentUnitServiceName == null) {
            throw new IllegalArgumentException("deploymentUnitServiceName is null");
        }
        this.componentName = componentName;
        this.componentClassName = componentClassName;
        configurators.addLast(FIRST_CONFIGURATOR);
    }

    /**
     * Get the component name.
     *
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Get the base service name for this component.
     *
     * @return the base service name
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    /**
     * Get the component instance class name.
     *
     * @return the component class name
     */
    public String getComponentClassName() {
        return componentClassName;
    }

    /**
     * Get the component's module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return moduleDescription.getModuleName();
    }

    /**
     * Get the component's module's application name.
     *
     * @return the application name
     */
    public String getApplicationName() {
        return moduleDescription.getApplicationName();
    }

    /**
     * Get the map of interceptor classes applied directly to class. These interceptors will have lifecycle methods invoked
     *
     * @return the interceptor classes
     */
    public List<InterceptorDescription> getClassInterceptors() {
        return classInterceptors;
    }

    /**
     * Returns a combined map of class and method level interceptors
     *
     * @return all interceptors on the class
     */
    public Map<String, InterceptorDescription> getAllInterceptors() {
        return allInterceptors;
    }

    /**
     * @return <code>true</code> if the <code>ExcludeDefaultInterceptors</code> annotation was applied to the class
     */
    public boolean isExcludeDefaultInterceptors() {
        return excludeDefaultInterceptors;
    }

    public void setExcludeDefaultInterceptors(boolean excludeDefaultInterceptors) {
        this.excludeDefaultInterceptors = excludeDefaultInterceptors;
    }

    /**
     * @param method The method that has been annotated <code>@ExcludeDefaultInterceptors</code>
     */
    public void excludeDefaultInterceptors(MethodIdentifier method) {
        methodExcludeDefaultInterceptors.add(method);
    }

    public boolean isExcludeDefaultInterceptors(MethodIdentifier method) {
        return methodExcludeDefaultInterceptors.contains(method);
    }

    /**
     * @param method The method that has been annotated <code>@ExcludeClassInterceptors</code>
     */
    public void excludeClassInterceptors(MethodIdentifier method) {
        methodExcludeClassInterceptors.add(method);
    }

    public boolean isExcludeClassInterceptors(MethodIdentifier method) {
        return methodExcludeClassInterceptors.contains(method);
    }

    /**
     * Add a class level interceptor.
     *
     * @param description the interceptor class description
     * @return {@code true} if the class interceptor was not already defined, {@code false} if it was
     */
    public boolean addClassInterceptor(InterceptorDescription description) {
        String name = description.getInterceptorClassName();
        if (classInterceptorsSet.contains(name)) {
            return false;
        }
        if (!allInterceptors.containsKey(name)) {
            allInterceptors.put(name, description);
        }
        classInterceptors.add(description);
        classInterceptorsSet.add(name);
        return true;
    }

    /**
     * Returns the {@link InterceptorDescription} for the passed <code>interceptorClassName</code>, if such a class
     * interceptor exists for this component description. Else returns null.
     *
     * @param interceptorClassName The fully qualified interceptor class name
     * @return
     */
    public InterceptorDescription getClassInterceptor(String interceptorClassName) {
        if (! classInterceptorsSet.contains(interceptorClassName)) {
            return null;
        }
        for (InterceptorDescription interceptor : classInterceptors) {
            if (interceptor.getInterceptorClassName().equals(interceptorClassName)) {
                return interceptor;
            }
        }
        return null;
    }

    /**
     * Get the method interceptor configurations.  The key is the method identifier, the value is
     * the set of class names of interceptors to configure on that method.
     *
     * @return the method interceptor configurations
     */
    public Map<MethodIdentifier, List<InterceptorDescription>> getMethodInterceptors() {
        return methodInterceptors;
    }

    /**
     * Add a method interceptor class name.
     *
     * @param method      the method
     * @param description the interceptor descriptor
     * @return {@code true} if the interceptor class was not already associated with the method, {@code false} if it was
     */
    public boolean addMethodInterceptor(MethodIdentifier method, InterceptorDescription description) {
        //we do not add method level interceptors to the set of interceptor classes,
        //as their around invoke annotations
        List<InterceptorDescription> interceptors = methodInterceptors.get(method);
        Set<String> interceptorClasses = methodInterceptorsSet.get(method);
        if (interceptors == null) {
            methodInterceptors.put(method, interceptors = new ArrayList<InterceptorDescription>());
            methodInterceptorsSet.put(method, interceptorClasses = new HashSet<String>());
        }
        final String name = description.getInterceptorClassName();
        if (interceptorClasses.contains(name)) {
            return false;
        }
        if (!allInterceptors.containsKey(name)) {
            allInterceptors.put(name, description);
        }
        interceptors.add(description);
        interceptorClasses.add(name);
        return true;
    }

    /**
     * Get the naming mode of this component.
     *
     * @return the naming mode
     */
    public ComponentNamingMode getNamingMode() {
        return namingMode;
    }

    /**
     * Set the naming mode of this component.  May not be {@code null}.
     *
     * @param namingMode the naming mode
     */
    public void setNamingMode(final ComponentNamingMode namingMode) {
        if (namingMode == null) {
            throw new IllegalArgumentException("namingMode is null");
        }
        this.namingMode = namingMode;
    }

    public EEModuleDescription getModuleDescription() {
        return moduleDescription;
    }

    public EEModuleClassDescription getClassDescription() {
        return classDescription;
    }

    /**
     * Add a dependency to this component.  If the same dependency is added multiple times, only the first will
     * take effect.
     *
     * @param serviceName the service name of the dependency
     * @param type        the type of the dependency (required or optional)
     */
    public void addDependency(ServiceName serviceName, ServiceBuilder.DependencyType type) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }
        final Map<ServiceName, ServiceBuilder.DependencyType> dependencies = this.dependencies;
        final ServiceBuilder.DependencyType dependencyType = dependencies.get(serviceName);
        if (dependencyType == ServiceBuilder.DependencyType.REQUIRED) {
            dependencies.put(serviceName, ServiceBuilder.DependencyType.REQUIRED);
        } else {
            dependencies.put(serviceName, type);
        }
    }

    /**
     * Get the dependency map.
     *
     * @return the dependency map
     */
    public Map<ServiceName, ServiceBuilder.DependencyType> getDependencies() {
        return dependencies;
    }

    public DeploymentDescriptorEnvironment getDeploymentDescriptorEnvironment() {
        return deploymentDescriptorEnvironment;
    }

    public void setDeploymentDescriptorEnvironment(DeploymentDescriptorEnvironment deploymentDescriptorEnvironment) {
        this.deploymentDescriptorEnvironment = deploymentDescriptorEnvironment;
    }

    /**
     * Get the list of views which apply to this component.
     *
     * @return the list of views
     */
    public List<ViewDescription> getViews() {
        return views;
    }

    /**
     * Get the configurators for this component.
     *
     * @return the configurators
     */
    public Deque<ComponentConfigurator> getConfigurators() {
        return configurators;
    }

    private static class DefaultFirstConfigurator implements ComponentConfigurator {

        public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            DeploymentUnit deploymentUnit = context.getDeploymentUnit();
            final DeploymentReflectionIndex index = deploymentUnit.getAttachment(REFLECTION_INDEX);
            final Object instanceKey = BasicComponentInstance.INSTANCE_KEY;

            // Module stuff
            final EEModuleClassDescription componentClassDescription = description.getClassDescription();
            final EEModuleClassConfiguration componentClassConfiguration = configuration.getModuleClassConfiguration();
            final EEModuleConfiguration moduleConfiguration = componentClassConfiguration.getModuleConfiguration();

            final Deque<InterceptorFactory> instantiators = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> injectors = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> uninjectors = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> destructors = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> userPostConstruct = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> userPreDestroy = new ArrayDeque<InterceptorFactory>();

            final ClassReflectionIndex<?> componentClassIndex = index.getClassIndex(componentClassConfiguration.getModuleClass());
            final InterceptorFactory componentUserAroundInvoke;
            final Map<String, InterceptorFactory> userAroundInvokesByInterceptorClass = new HashMap<String, InterceptorFactory>();

            // Primary instance
            instantiators.addFirst(new ManagedReferenceInterceptorFactory(configuration.getInstanceFactory(), instanceKey));
            destructors.addLast(new ManagedReferenceReleaseInterceptorFactory(instanceKey));
            for (final ResourceInjectionConfiguration injectionConfiguration : componentClassConfiguration.getInjectionConfigurations()) {
                final Object valueContextKey = new Object();
                final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue = new InjectedValue<ManagedReferenceFactory>();
                configuration.getStartDependencies().add(new InjectedConfigurator(injectionConfiguration, configuration, context, managedReferenceFactoryValue));
                injectors.addFirst(injectionConfiguration.getTarget().createInjectionInterceptorFactory(instanceKey, valueContextKey, managedReferenceFactoryValue, deploymentUnit));
                uninjectors.addLast(new ManagedReferenceReleaseInterceptorFactory(valueContextKey));
            }
            final MethodIdentifier componentPostConstructMethod = componentClassDescription.getPostConstructMethod();
            if (componentPostConstructMethod != null) {
                Method method = componentClassIndex.getMethod(componentPostConstructMethod);
                InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, true);
                userPostConstruct.addLast(interceptorFactory);
            }
            final MethodIdentifier componentPreDestroyMethod = componentClassDescription.getPreDestroyMethod();
            if (componentPreDestroyMethod != null) {
                Method method = componentClassIndex.getMethod(componentPreDestroyMethod);
                InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, true);
                userPreDestroy.addLast(interceptorFactory);
            }
            final MethodIdentifier componentAroundInvokeMethod = componentClassDescription.getAroundInvokeMethod();
            if (componentAroundInvokeMethod != null) {
                Method method = componentClassIndex.getMethod(componentAroundInvokeMethod);
                componentUserAroundInvoke = new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, false);
            } else {
                componentUserAroundInvoke = null;
            }

            // Interceptor instances
            final Map<String, InterceptorDescription> interceptors = description.getAllInterceptors();
            for (InterceptorDescription interceptorDescription : interceptors.values()) {
                final String interceptorClassName = interceptorDescription.getInterceptorClassName();
                final EEModuleClassConfiguration interceptorClassConfiguration = moduleConfiguration.getClassConfiguration(interceptorClassName);
                final Object contextKey = new Object();
                instantiators.addFirst(new ManagedReferenceInterceptorFactory(interceptorClassConfiguration.getInstantiator(), contextKey));
                destructors.addLast(new ManagedReferenceReleaseInterceptorFactory(contextKey));
                for (final ResourceInjectionConfiguration injectionConfiguration : interceptorClassConfiguration.getInjectionConfigurations()) {
                    final Object valueContextKey = new Object();
                    final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue = new InjectedValue<ManagedReferenceFactory>();
                    configuration.getStartDependencies().add(new InjectedConfigurator(injectionConfiguration, configuration, context, managedReferenceFactoryValue));
                    injectors.addFirst(injectionConfiguration.getTarget().createInjectionInterceptorFactory(contextKey, valueContextKey, managedReferenceFactoryValue, deploymentUnit));
                    uninjectors.addLast(new ManagedReferenceReleaseInterceptorFactory(valueContextKey));
                }
                final MethodIdentifier postConstructMethod = componentClassDescription.getPostConstructMethod();
                if (postConstructMethod != null) {
                    Method method = componentClassIndex.getMethod(postConstructMethod);
                    InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, true);
                    userPostConstruct.addLast(interceptorFactory);
                }
                final MethodIdentifier preDestroyMethod = componentClassDescription.getPreDestroyMethod();
                if (preDestroyMethod != null) {
                    Method method = componentClassIndex.getMethod(preDestroyMethod);
                    InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, true);
                    userPreDestroy.addLast(interceptorFactory);
                }
                final MethodIdentifier aroundInvokeMethod = componentClassDescription.getAroundInvokeMethod();
                if (aroundInvokeMethod != null) {
                    Method method = componentClassIndex.getMethod(aroundInvokeMethod);
                    userAroundInvokesByInterceptorClass.put(interceptorClassName, new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, false));
                }
            }

            // Apply post-construct
            final Deque<InterceptorFactory> postConstructInterceptors = configuration.getPostConstructInterceptors();
            final Iterator<InterceptorFactory> injectorIterator = injectors.descendingIterator();
            while (injectorIterator.hasNext()) {
                postConstructInterceptors.addFirst(injectorIterator.next());
            }
            final Iterator<InterceptorFactory> instantiatorIterator = instantiators.descendingIterator();
            while (instantiatorIterator.hasNext()) {
                postConstructInterceptors.addFirst(instantiatorIterator.next());
            }
            postConstructInterceptors.addAll(userPostConstruct);

            // Apply pre-destroy
            final Deque<InterceptorFactory> preDestroyInterceptors = configuration.getPreDestroyInterceptors();
            final Iterator<InterceptorFactory> uninjectorsIterator = uninjectors.descendingIterator();
            while (uninjectorsIterator.hasNext()) {
                preDestroyInterceptors.addFirst(uninjectorsIterator.next());
            }
            final Iterator<InterceptorFactory> destructorIterator = destructors.descendingIterator();
            while (destructorIterator.hasNext()) {
                preDestroyInterceptors.addFirst(destructorIterator.next());
            }
            preDestroyInterceptors.addAll(userPreDestroy);

            // Method interceptors
            final List<InterceptorDescription> classInterceptors = description.getClassInterceptors();
            final Set<String> visited = new HashSet<String>();
            final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = description.getMethodInterceptors();
            for (MethodIdentifier identifier : methodInterceptors.keySet()) {
                final List<InterceptorDescription> descriptions = methodInterceptors.get(identifier);
                final Method componentMethod = componentClassIndex.getMethod(identifier);
                final Deque<InterceptorFactory> interceptorDeque = configuration.getComponentInterceptorDeque(componentMethod);
                // TODO - ordering...?
                for (InterceptorDescription interceptorDescription : descriptions) {
                    String interceptorClassName = interceptorDescription.getInterceptorClassName();
                    if (visited.add(interceptorClassName)) {
                        interceptorDeque.addLast(userAroundInvokesByInterceptorClass.get(interceptorClassName));
                    }
                }
                if (! description.isExcludeClassInterceptors(identifier)) {
                    for (InterceptorDescription interceptorDescription : classInterceptors) {
                        String interceptorClassName = interceptorDescription.getInterceptorClassName();
                        if (visited.add(interceptorClassName)) {
                            interceptorDeque.addLast(userAroundInvokesByInterceptorClass.get(interceptorClassName));
                        }
                    }
                    if (componentUserAroundInvoke != null) {
                        interceptorDeque.addLast(componentUserAroundInvoke);
                    }
                }
                if (! description.isExcludeDefaultInterceptors() && ! description.isExcludeDefaultInterceptors(identifier)) {
                    // todo: default interceptors here
                }
                visited.clear();
                interceptorDeque.addLast(new ManagedReferenceMethodInterceptorFactory(instanceKey, componentMethod));
            }
        }
    }

    static class InjectedConfigurator implements DependencyConfigurator {

        private final ResourceInjectionConfiguration injectionConfiguration;
        private final ComponentConfiguration configuration;
        private final DeploymentPhaseContext context;
        private final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue;

        InjectedConfigurator(final ResourceInjectionConfiguration injectionConfiguration, final ComponentConfiguration configuration, final DeploymentPhaseContext context, final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue) {
            this.injectionConfiguration = injectionConfiguration;
            this.configuration = configuration;
            this.context = context;
            this.managedReferenceFactoryValue = managedReferenceFactoryValue;
        }

        public void configureDependency(final ServiceBuilder<?> serviceBuilder) throws DeploymentUnitProcessingException {
            injectionConfiguration.getSource().getResourceValue(configuration, serviceBuilder, context, managedReferenceFactoryValue);
        }
    }
}
