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

import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * A description of a generic Java EE component.  The description is pre-classloading so it references everything by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ComponentDescription {

    private static final DefaultFirstConfigurator FIRST_CONFIGURATOR = new DefaultFirstConfigurator();

    private static final AtomicInteger PROXY_ID = new AtomicInteger(0);

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    private final ServiceName serviceName;
    private final String componentName;
    private final String componentClassName;
    private final EEApplicationClasses applicationClassesDescription;
    private final EEModuleDescription moduleDescription;
    private final EEModuleClassDescription classDescription;

    private List<InterceptorDescription> classInterceptors = new ArrayList<InterceptorDescription>();
    private List<InterceptorDescription> defaultInterceptors = new ArrayList<InterceptorDescription>();

    private final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = new HashMap<MethodIdentifier, List<InterceptorDescription>>();
    private final Map<MethodIdentifier, Set<String>> methodInterceptorsSet = new HashMap<MethodIdentifier, Set<String>>();

    private final Set<MethodIdentifier> methodExcludeDefaultInterceptors = new HashSet<MethodIdentifier>();
    private final Set<MethodIdentifier> methodExcludeClassInterceptors = new HashSet<MethodIdentifier>();

    private final Map<ServiceName, ServiceBuilder.DependencyType> dependencies = new HashMap<ServiceName, ServiceBuilder.DependencyType>();

    private Set<InterceptorDescription> allInterceptors;

    private ComponentNamingMode namingMode = ComponentNamingMode.USE_MODULE;
    private boolean excludeDefaultInterceptors = false;
    private DeploymentDescriptorEnvironment deploymentDescriptorEnvironment;

    private final Set<ViewDescription> views = new HashSet<ViewDescription>();

    // Bindings
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();

    private final Deque<ComponentConfigurator> configurators = new ArrayDeque<ComponentConfigurator>();

    /**
     * If this component is deployed in a bean deployment archive this stores the id of the BDA
     */
    private String beanDeploymentArchiveId;

    /**
     * Construct a new instance.
     *
     * @param componentName             the component name
     * @param componentClassName        the component instance class name
     * @param moduleDescription         the EE module description
     * @param classDescription          the component class' description
     * @param deploymentUnitServiceName the service name of the DU containing this component
     * @param applicationClassesDescription
     */
    public ComponentDescription(final String componentName, final String componentClassName, final EEModuleDescription moduleDescription, final EEModuleClassDescription classDescription, final ServiceName deploymentUnitServiceName, final EEApplicationClasses applicationClassesDescription) {
        this.moduleDescription = moduleDescription;
        this.classDescription = classDescription;
        this.applicationClassesDescription = applicationClassesDescription;
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
        serviceName = deploymentUnitServiceName.append("component").append(componentName);
        this.componentName = componentName;
        this.componentClassName = componentClassName;
        configurators.addLast(FIRST_CONFIGURATOR);
    }

    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {
        return new ComponentConfiguration(this, applicationDescription.getClassConfiguration(this.getComponentClassName()));
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
     * Get the list of interceptor classes applied directly to class. These interceptors will have lifecycle methods invoked
     *
     * @return the interceptor classes
     */
    public List<InterceptorDescription> getClassInterceptors() {
        return classInterceptors;
    }

    /**
     * Override the class interceptors with a new set of interceptors
     *
     * @param classInterceptors
     */
    public void setClassInterceptors(List<InterceptorDescription> classInterceptors) {
        for (InterceptorDescription clazz : classInterceptors) {
            applicationClassesDescription.getOrAddClassByName(clazz.getInterceptorClassName());
        }
        this.classInterceptors = classInterceptors;
        this.allInterceptors = null;
    }


    /**
     * @return the components default interceptors
     */
    public List<InterceptorDescription> getDefaultInterceptors() {
        return defaultInterceptors;
    }

    public void setDefaultInterceptors(final List<InterceptorDescription> defaultInterceptors) {
        allInterceptors = null;
        this.defaultInterceptors = defaultInterceptors;
    }

    /**
     * Returns a combined map of class and method level interceptors
     *
     * @return all interceptors on the class
     */
    public Set<InterceptorDescription> getAllInterceptors() {
        if (allInterceptors == null) {
            allInterceptors = new HashSet<InterceptorDescription>();
            allInterceptors.addAll(classInterceptors);
            if (!excludeDefaultInterceptors) {
                allInterceptors.addAll(defaultInterceptors);
            }
            for (List<InterceptorDescription> interceptors : methodInterceptors.values()) {
                allInterceptors.addAll(interceptors);
            }
        }
        return allInterceptors;
    }

    /**
     * @return <code>true</code> if the <code>ExcludeDefaultInterceptors</code> annotation was applied to the class
     */
    public boolean isExcludeDefaultInterceptors() {
        return excludeDefaultInterceptors;
    }

    public void setExcludeDefaultInterceptors(boolean excludeDefaultInterceptors) {
        allInterceptors = null;
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
        // add the interceptor class to the EEModuleDescription
        this.applicationClassesDescription.getOrAddClassByName(name);
        if (classInterceptors.contains(description)) {
            return false;
        }
        classInterceptors.add(description);
        this.allInterceptors = null;
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
        // add the interceptor class to the EEModuleDescription
        this.applicationClassesDescription.getOrAddClassByName(name);
        if (interceptorClasses.contains(name)) {
            return false;
        }
        interceptors.add(description);
        interceptorClasses.add(name);
        this.allInterceptors = null;
        return true;
    }

    /**
     * Sets the method level interceptors for a method, and marks it as exclude class and default level interceptors.
     * <p/>
     * This is used to set the final interceptor order after it has been modifier by the deployment descriptor
     *
     * @param identifier
     * @param interceptorDescriptions
     */
    public void setMethodInterceptors(MethodIdentifier identifier, List<InterceptorDescription> interceptorDescriptions) {
        methodInterceptors.put(identifier, interceptorDescriptions);
        methodExcludeClassInterceptors.add(identifier);
        methodExcludeDefaultInterceptors.add(identifier);
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
     * Get the binding configurations for this component.  This list contains bindings which are specific to the
     * component.
     *
     * @return the binding configurations
     */
    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
    }

    /**
     * Get the list of views which apply to this component.
     *
     * @return the list of views
     */
    public Set<ViewDescription> getViews() {
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

    private static InterceptorFactory weaved(final Collection<InterceptorFactory> interceptorFactories) {
        return new InterceptorFactory() {
            @Override
            public Interceptor create(InterceptorFactoryContext context) {
                final Interceptor[] interceptors = new Interceptor[interceptorFactories.size()];
                final Iterator<InterceptorFactory> factories = interceptorFactories.iterator();
                for (int i = 0; i < interceptors.length; i++) {
                    interceptors[i] = factories.next().create(context);
                }
                return Interceptors.getWeavedInterceptor(interceptors);
            }
        };
    }

    private static class DefaultFirstConfigurator implements ComponentConfigurator {

        public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
            final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(REFLECTION_INDEX);
            final Object instanceKey = BasicComponentInstance.INSTANCE_KEY;
            final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
            final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);

            // Module stuff
            final EEModuleClassConfiguration componentClassConfiguration = configuration.getModuleClassConfiguration();

            final Deque<InterceptorFactory> instantiators = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> injectors = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> uninjectors = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> destructors = new ArrayDeque<InterceptorFactory>();

            final ClassReflectionIndex<?> componentClassIndex = deploymentReflectionIndex.getClassIndex(componentClassConfiguration.getModuleClass());
            final List<InterceptorFactory> componentUserAroundInvoke = new ArrayList<InterceptorFactory>();
            final Map<String, List<InterceptorFactory>> userAroundInvokesByInterceptorClass = new HashMap<String, List<InterceptorFactory>>();

            final Map<String, List<InterceptorFactory>> userPostConstructByInterceptorClass = new HashMap<String, List<InterceptorFactory>>();
            final Map<String, List<InterceptorFactory>> userPreDestroyByInterceptorClass = new HashMap<String, List<InterceptorFactory>>();

            // Primary instance
            final ManagedReferenceFactory instanceFactory = configuration.getInstanceFactory();
            if (instanceFactory != null) {
                instantiators.addFirst(new ManagedReferenceInterceptorFactory(instanceFactory, instanceKey));
            } else {
                //use the default constructor if no instanceFactory has been set
                final Constructor<Object> constructor = (Constructor<Object>) componentClassIndex.getConstructor(EMPTY_CLASS_ARRAY);
                if (constructor == null) {
                    throw new DeploymentUnitProcessingException("Could not find default constructor for " + componentClassConfiguration.getModuleClass());
                }
                ValueManagedReferenceFactory factory = new ValueManagedReferenceFactory(new ConstructedValue<Object>(constructor, Collections.<Value<?>>emptyList()));
                instantiators.addFirst(new ManagedReferenceInterceptorFactory(factory, instanceKey));
            }
            destructors.addLast(new ManagedReferenceReleaseInterceptorFactory(instanceKey));

            new ClassDescriptionTraversal(componentClassConfiguration, applicationDescription) {

                @Override
                public void handle(EEModuleClassConfiguration classConfiguration, EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                    for (final ResourceInjectionConfiguration injectionConfiguration : classConfiguration.getInjectionConfigurations()) {
                        final Object valueContextKey = new Object();
                        final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue = new InjectedValue<ManagedReferenceFactory>();
                        configuration.getStartDependencies().add(new InjectedConfigurator(injectionConfiguration, configuration, context, managedReferenceFactoryValue));
                        injectors.addFirst(injectionConfiguration.getTarget().createInjectionInterceptorFactory(instanceKey, valueContextKey, managedReferenceFactoryValue, deploymentUnit));
                        uninjectors.addLast(new ManagedReferenceReleaseInterceptorFactory(valueContextKey));
                    }
                }
            }.run();


            //all interceptors with lifecycle callbacks, in the correct order
            final LinkedHashSet<InterceptorDescription> interceptorWithLifecycleCallbacks = new LinkedHashSet<InterceptorDescription>();
            if (!description.isExcludeDefaultInterceptors()) {
                interceptorWithLifecycleCallbacks.addAll(description.getDefaultInterceptors());
            }
            interceptorWithLifecycleCallbacks.addAll(description.getClassInterceptors());

            for (final InterceptorDescription interceptorDescription : description.getAllInterceptors()) {
                final String interceptorClassName = interceptorDescription.getInterceptorClassName();
                final EEModuleClassConfiguration interceptorConfiguration = applicationDescription.getClassConfiguration(interceptorClassName);

                //we store the interceptor instance under the class key
                final Object contextKey = interceptorConfiguration.getModuleClass();
                if (interceptorConfiguration.getInstantiator() == null) {
                    throw new DeploymentUnitProcessingException("No default constructor for interceptor class " + interceptorClassName + " on component " + componentClassConfiguration.getModuleClass());
                }
                instantiators.addFirst(new ManagedReferenceInterceptorFactory(interceptorConfiguration.getInstantiator(), contextKey));
                destructors.addLast(new ManagedReferenceReleaseInterceptorFactory(contextKey));

                final boolean interceptorHasLifecycleCallbacks = interceptorWithLifecycleCallbacks.contains(interceptorDescription);
                final ClassReflectionIndex<?> interceptorIndex = deploymentReflectionIndex.getClassIndex(interceptorConfiguration.getModuleClass());

                new ClassDescriptionTraversal(interceptorConfiguration, applicationDescription) {
                    @Override
                    public void handle(EEModuleClassConfiguration interceptorClassConfiguration, EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                        final ClassReflectionIndex<?> interceptorClassIndex = deploymentReflectionIndex.getClassIndex(interceptorClassConfiguration.getModuleClass());

                        for (final ResourceInjectionConfiguration injectionConfiguration : interceptorClassConfiguration.getInjectionConfigurations()) {
                            final Object valueContextKey = new Object();
                            final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue = new InjectedValue<ManagedReferenceFactory>();
                            configuration.getStartDependencies().add(new InjectedConfigurator(injectionConfiguration, configuration, context, managedReferenceFactoryValue));
                            injectors.addFirst(injectionConfiguration.getTarget().createInjectionInterceptorFactory(contextKey, valueContextKey, managedReferenceFactoryValue, deploymentUnit));
                            uninjectors.addLast(new ManagedReferenceReleaseInterceptorFactory(valueContextKey));
                        }
                        // Only class level interceptors are processed for postconstruct/predestroy methods.
                        // Method level interceptors aren't supposed to be processed for postconstruct/predestroy lifecycle
                        // methods, as per interceptors spec
                        if (interceptorHasLifecycleCallbacks) {
                            final MethodIdentifier postConstructMethodIdentifier = classDescription.getPostConstructMethod();
                            if (postConstructMethodIdentifier != null) {
                                final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, interceptorClassIndex, postConstructMethodIdentifier);

                                if (isNotOverriden(interceptorClassConfiguration, method, interceptorIndex, deploymentReflectionIndex)) {
                                    InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, true);
                                    List<InterceptorFactory> userPostConstruct = userPostConstructByInterceptorClass.get(interceptorClassName);
                                    if (userPostConstruct == null) {
                                        userPostConstructByInterceptorClass.put(interceptorClassName, userPostConstruct = new ArrayList<InterceptorFactory>());
                                    }
                                    userPostConstruct.add(interceptorFactory);
                                }
                            }
                            final MethodIdentifier preDestroyMethodIdentifier = classDescription.getPreDestroyMethod();
                            if (preDestroyMethodIdentifier != null) {
                                final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, interceptorClassIndex, preDestroyMethodIdentifier);
                                if (isNotOverriden(interceptorClassConfiguration, method, interceptorIndex, deploymentReflectionIndex)) {
                                    InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, true);
                                    List<InterceptorFactory> userPreDestroy = userPreDestroyByInterceptorClass.get(interceptorClassName);
                                    if (userPreDestroy == null) {
                                        userPreDestroyByInterceptorClass.put(interceptorClassName, userPreDestroy = new ArrayList<InterceptorFactory>());
                                    }
                                    userPreDestroy.add(interceptorFactory);
                                }
                            }
                        }
                        final MethodIdentifier aroundInvokeMethodIdentifier = classDescription.getAroundInvokeMethod();
                        if (aroundInvokeMethodIdentifier != null) {
                            final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, interceptorClassIndex, aroundInvokeMethodIdentifier);
                            if (isNotOverriden(interceptorClassConfiguration, method, interceptorIndex, deploymentReflectionIndex)) {
                                List<InterceptorFactory> interceptors;
                                if ((interceptors = userAroundInvokesByInterceptorClass.get(interceptorClassName)) == null) {
                                    userAroundInvokesByInterceptorClass.put(interceptorClassName, interceptors = new ArrayList<InterceptorFactory>());
                                }
                                interceptors.add(new ManagedReferenceLifecycleMethodInterceptorFactory(contextKey, method, false));
                            }
                        }
                    }
                }.run();
            }

            final Deque<InterceptorFactory> userPostConstruct = new ArrayDeque<InterceptorFactory>();
            final Deque<InterceptorFactory> userPreDestroy = new ArrayDeque<InterceptorFactory>();

            //now add the lifecycle interceptors in the correct order


            for (final InterceptorDescription interceptorClass : interceptorWithLifecycleCallbacks) {
                if (userPostConstructByInterceptorClass.containsKey(interceptorClass.getInterceptorClassName())) {
                    userPostConstruct.addAll(userPostConstructByInterceptorClass.get(interceptorClass.getInterceptorClassName()));
                }
                if (userPreDestroyByInterceptorClass.containsKey(interceptorClass.getInterceptorClassName())) {
                    userPreDestroy.addAll(userPreDestroyByInterceptorClass.get(interceptorClass.getInterceptorClassName()));
                }
            }


            new ClassDescriptionTraversal(componentClassConfiguration, applicationDescription) {
                @Override
                public void handle(EEModuleClassConfiguration configuration, EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                    final ClassReflectionIndex classReflectionIndex = deploymentReflectionIndex.getClassIndex(configuration.getModuleClass());
                    final MethodIdentifier componentPostConstructMethodIdentifier = classDescription.getPostConstructMethod();
                    if (componentPostConstructMethodIdentifier != null) {
                        final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, classReflectionIndex, componentPostConstructMethodIdentifier);
                        if (isNotOverriden(configuration, method, componentClassIndex, deploymentReflectionIndex)) {
                            InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, true);
                            userPostConstruct.addLast(interceptorFactory);
                        }
                    }
                    final MethodIdentifier componentPreDestroyMethodIdentifier = classDescription.getPreDestroyMethod();
                    if (componentPreDestroyMethodIdentifier != null) {
                        final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, classReflectionIndex, componentPreDestroyMethodIdentifier);
                        if (isNotOverriden(configuration, method, componentClassIndex, deploymentReflectionIndex)) {
                            InterceptorFactory interceptorFactory = new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, true);
                            userPreDestroy.addLast(interceptorFactory);
                        }
                    }
                    final MethodIdentifier componentAroundInvokeMethodIdentifier = classDescription.getAroundInvokeMethod();
                    if (componentAroundInvokeMethodIdentifier != null) {
                        final Method method = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, classReflectionIndex, componentAroundInvokeMethodIdentifier);

                        if (isNotOverriden(configuration, method, componentClassIndex, deploymentReflectionIndex)) {
                            componentUserAroundInvoke.add(new ManagedReferenceLifecycleMethodInterceptorFactory(instanceKey, method, false));
                        }
                    }
                }
            }.run();

            final InterceptorFactory tcclInterceptor = new ImmediateInterceptorFactory(new TCCLInterceptor(module.getClassLoader()));

            // Apply post-construct
            if (!injectors.isEmpty()) {
                configuration.addPostConstructInterceptor(weaved(injectors), InterceptorOrder.ComponentPostConstruct.RESOURCE_INJECTION_INTERCEPTORS);
            }

            if (!instantiators.isEmpty()) {
                configuration.addPostConstructInterceptor(weaved(instantiators), InterceptorOrder.ComponentPostConstruct.INSTANTIATION_INTERCEPTORS);
            }
            if (!userPostConstruct.isEmpty()) {
                configuration.addPostConstructInterceptor(weaved(userPostConstruct), InterceptorOrder.ComponentPostConstruct.USER_INTERCEPTORS);
            }
            configuration.addPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ComponentPostConstruct.TERMINAL_INTERCEPTOR);
            configuration.addPostConstructInterceptor(tcclInterceptor, InterceptorOrder.ComponentPostConstruct.TCCL_INTERCEPTOR);

            // Apply pre-destroy
            if (!uninjectors.isEmpty()) {
                configuration.addPreDestroyInterceptor(weaved(uninjectors), InterceptorOrder.ComponentPreDestroy.UNINJECTION_INTERCEPTORS);
            }
            if (!destructors.isEmpty()) {
                configuration.addPreDestroyInterceptor(weaved(destructors), InterceptorOrder.ComponentPreDestroy.DESTRUCTION_INTERCEPTORS);
            }
            if (!userPreDestroy.isEmpty()) {
                configuration.addPreDestroyInterceptor(weaved(userPreDestroy), InterceptorOrder.ComponentPreDestroy.USER_INTERCEPTORS);
            }

            configuration.addPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ComponentPreDestroy.TERMINAL_INTERCEPTOR);
            configuration.addPreDestroyInterceptor(tcclInterceptor, InterceptorOrder.ComponentPreDestroy.TCCL_INTERCEPTOR);

            // @AroundInvoke interceptors
            final List<InterceptorDescription> classInterceptors = description.getClassInterceptors();
            final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = description.getMethodInterceptors();

            for (final Method method : componentClassConfiguration.getClassMethods()) {

                //now add the interceptor that initializes and the interceptor that actually invokes to the end of the interceptor chain

                configuration.addComponentInterceptor(method, Interceptors.getInitialInterceptorFactory(), InterceptorOrder.Component.INITIAL_INTERCEPTOR);
                configuration.addComponentInterceptor(method, new ManagedReferenceMethodInterceptorFactory(instanceKey, method), InterceptorOrder.Component.TERMINAL_INTERCEPTOR);
                // and also add the tccl interceptor
                configuration.addComponentInterceptor(method, tcclInterceptor, InterceptorOrder.Component.TCCL_INTERCEPTOR);


                final MethodIdentifier identifier = MethodIdentifier.getIdentifier(method.getReturnType(), method.getName(), method.getParameterTypes());

                final List<InterceptorFactory> userAroundInvokes = new ArrayList<InterceptorFactory>();
                // first add the default interceptors (if not excluded) to the deque
                if (!description.isExcludeDefaultInterceptors() && !description.isExcludeDefaultInterceptors(identifier)) {
                    for (InterceptorDescription interceptorDescription : description.getDefaultInterceptors()) {
                        String interceptorClassName = interceptorDescription.getInterceptorClassName();
                        List<InterceptorFactory> aroundInvokes = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                        if (aroundInvokes != null) {
                            userAroundInvokes.addAll(aroundInvokes);
                        }
                    }
                }

                // now add class level interceptors (if not excluded) to the deque
                if (!description.isExcludeClassInterceptors(identifier)) {
                    for (InterceptorDescription interceptorDescription : classInterceptors) {
                        String interceptorClassName = interceptorDescription.getInterceptorClassName();
                        List<InterceptorFactory> aroundInvokes = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                        if (aroundInvokes != null) {
                            if (aroundInvokes != null) {
                                userAroundInvokes.addAll(aroundInvokes);
                            }
                        }
                    }
                }

                // now add method level interceptors for to the deque so that they are triggered after the class interceptors
                List<InterceptorDescription> methodLevelInterceptors = methodInterceptors.get(identifier);
                if (methodLevelInterceptors != null) {
                    for (InterceptorDescription methodLevelInterceptor : methodLevelInterceptors) {
                        String interceptorClassName = methodLevelInterceptor.getInterceptorClassName();
                        List<InterceptorFactory> aroundInvokes = userAroundInvokesByInterceptorClass.get(interceptorClassName);
                        if (aroundInvokes != null) {
                            if (aroundInvokes != null) {
                                userAroundInvokes.addAll(aroundInvokes);
                            }
                        }

                    }
                }

                // finally add the component level around invoke to the deque so that it's triggered last
                if (componentUserAroundInvoke != null) {
                    userAroundInvokes.addAll(componentUserAroundInvoke);
                }

                if (!userAroundInvokes.isEmpty()) {
                    configuration.addComponentInterceptor(method, weaved(userAroundInvokes), InterceptorOrder.Component.USER_INTERCEPTORS);
                }
            }


            //views
            for (ViewDescription view : description.getViews()) {
                Class<?> viewClass;
                try {
                    viewClass = module.getClassLoader().loadClass(view.getViewClassName());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load view class " + view.getViewClassName() + " for component " + configuration, e);
                }
                final ViewConfiguration viewConfiguration;
                if (viewClass.isInterface()) {
                    viewConfiguration = view.createViewConfiguration(viewClass, configuration, new ProxyFactory(viewClass.getName() + "$$$view" + PROXY_ID.incrementAndGet(), Object.class, viewClass.getClassLoader(), viewClass.getProtectionDomain(), viewClass));
                } else {
                    viewConfiguration = view.createViewConfiguration(viewClass, configuration, new ProxyFactory(viewClass.getName() + "$$$view" + PROXY_ID.incrementAndGet(), viewClass, viewClass.getClassLoader(), viewClass.getProtectionDomain()));
                }
                for (final ViewConfigurator configurator : view.getConfigurators()) {
                    configurator.configure(context, configuration, view, viewConfiguration);
                }
                configuration.getViews().add(viewConfiguration);
            }

            configuration.getStartDependencies().add(new DependencyConfigurator() {
                @Override
                public void configureDependency(final ServiceBuilder<?> serviceBuilder) throws DeploymentUnitProcessingException {
                    for (final Map.Entry<ServiceName, ServiceBuilder.DependencyType> entry : description.getDependencies().entrySet()) {
                        serviceBuilder.addDependency(entry.getValue(), entry.getKey());
                    }

                }
            });
        }

        private boolean isNotOverriden(final EEModuleClassConfiguration configuration, final Method method, final ClassReflectionIndex<?> componentClassIndex, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
            return Modifier.isPrivate(method.getModifiers()) || ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, componentClassIndex, method).getDeclaringClass() == configuration.getModuleClass();
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
            InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                    configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.USE_MODULE,
                    configuration.getComponentName(),
                    configuration.getModuleName(),
                    configuration.getApplicationName()
            );
            injectionConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, context, managedReferenceFactoryValue);
        }
    }

    public String getBeanDeploymentArchiveId() {
        return beanDeploymentArchiveId;
    }

    public void setBeanDeploymentArchiveId(final String beanDeploymentArchiveId) {
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
    }
}
