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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.MethodInterceptorFactory;
import org.jboss.invocation.MethodInvokingInterceptorFactory;
import org.jboss.invocation.SimpleInterceptorInstanceFactory;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A description of a generic Java EE component.  The description is pre-classloading so it references everything by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentDescription extends AbstractInjectableDescription {

    private final String componentName;
    private final String moduleName;
    private final String applicationName;
    private final String componentClassName;
    private final List<InterceptorMethodDescription> postConstructs = new ArrayList<InterceptorMethodDescription>();
    private final List<InterceptorMethodDescription> preDestroys = new ArrayList<InterceptorMethodDescription>();
    private final Map<String, InterceptorMethodDescription> aroundInvokeMethods = new LinkedHashMap<String,InterceptorMethodDescription>();

    private final List<InterceptorDescription> classInterceptors = new ArrayList<InterceptorDescription>();
    private final Set<String> classInterceptorsSet = new HashSet<String>();

    private final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = new HashMap<MethodIdentifier, List<InterceptorDescription>>();
    private final Map<MethodIdentifier, Set<String>> methodInterceptorsSet = new HashMap<MethodIdentifier, Set<String>>();

    private final Set<MethodIdentifier> methodExcludeDefaultInterceptors = new HashSet<MethodIdentifier>();
    private final Set<MethodIdentifier> methodExcludeClassInterceptors = new HashSet<MethodIdentifier>();

    private final Map<String,InterceptorDescription> allInterceptors = new HashMap<String,InterceptorDescription>();

    private final Map<ServiceName, ServiceBuilder.DependencyType> dependencies = new HashMap<ServiceName, ServiceBuilder.DependencyType>();

    private final Set<String> viewClassNames = new HashSet<String>();
    private ComponentNamingMode namingMode = ComponentNamingMode.NONE;
    private boolean excludeDefaultInterceptors = false;

    /**
     * Construct a new instance.
     *
     * @param componentName the component name
     * @param componentClassName the component instance class name
     * @param moduleName the module name
     * @param applicationName the application name
     */
    protected AbstractComponentDescription(final String componentName, final String componentClassName, final String moduleName, final String applicationName) {
        this.moduleName = moduleName;
        this.applicationName = applicationName;
        if (componentName == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (componentClassName == null) {
            throw new IllegalArgumentException("className is null");
        }
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName is null");
        }
        if (applicationName == null) {
            throw new IllegalArgumentException("applicationName is null");
        }
        this.componentName = componentName;
        this.componentClassName = componentClassName;
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
        return moduleName;
    }

    /**
     * Get the component's module's application name.
     *
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Adds a post construct method
     * @param methodDescription The method
     */
    public void addPostConstructMethod(InterceptorMethodDescription methodDescription) {
        postConstructs.add(methodDescription);
    }

    /**
     * Get the post-construct lifecycle method configurations.
     *
     * @return the post-construct lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getPostConstructs() {
        return postConstructs;
    }

    /**
     * Adds a pre destroy method
     * @param methodDescription The method
     */
    public void addPreDestroyMethod(InterceptorMethodDescription methodDescription) {
        preDestroys.add(methodDescription);
    }

    /**
     * Get the pre-destroy lifecycle method configurations.
     *
     * @return the pre-destroy lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getPreDestroys() {
        return preDestroys;
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
    public Map<String,InterceptorDescription> getAllInterceptors() {
        return allInterceptors;
    }

    /**
     *
     * @return <code>true</code> if the <code>ExcludeDefaultInterceptors</code> annotation was applied to the class
     */
    public boolean isExcludeDefaultInterceptors() {
        return excludeDefaultInterceptors;
    }

    public void setExcludeDefaultInterceptors(boolean excludeDefaultInterceptors) {
        this.excludeDefaultInterceptors = excludeDefaultInterceptors;
    }

    /**
     *
     * @param method The method that has been annotated <code>@ExcludeDefaultInterceptors</code>
     */
    public void excludeDefaultInterceptors(MethodIdentifier method) {
        methodExcludeDefaultInterceptors.add(method);
    }

    /**
     *
     * @param method The method that has been annotated <code>@ExcludeClassInterceptors</code>
     */
    public void excludeClassInterceptors(MethodIdentifier method) {
        methodExcludeClassInterceptors.add(method);
    }

    /**
     * Add a class level interceptor.
     *
     * @param description the interceptor class description
     * @return {@code true} if the class interceptor was not already defined, {@code false} if it was
     */
    public boolean addClassInterceptor(InterceptorDescription description) {
        String name = description.getInterceptorClassName();
        //TODO: this is O(n)
        if (classInterceptorsSet.contains(name)) {
            return false;
        }
        //TODO: so is this
        if(!allInterceptors.containsKey(name)) {
            allInterceptors.put(name,description);
        }
        classInterceptors.add(description);
        classInterceptorsSet.add(name);
        return true;
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
     * @param method the method
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
        if(interceptorClasses.contains(name)) {
            return false;
        }
        if(!allInterceptors.containsKey(name)) {
            allInterceptors.put(name, description);
        }
        interceptors.add(description);
        interceptorClasses.add(name);
        return true;
    }

    /**
     * Adds an AroundInvoke annotated method that was found on the component.
     *
     * @param methodDescription The method description
     */
    public void addAroundInvokeMethod(InterceptorMethodDescription methodDescription) {
        String declaringClassName = methodDescription.getDeclaringClass();
        if(aroundInvokeMethods.containsKey(declaringClassName)) {
            throw new IllegalArgumentException("Only one @AroundInvoke method allowed per class: " + declaringClassName);
        }
        aroundInvokeMethods.put(declaringClassName,methodDescription);
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

    /**
     * Get the view class names set.
     *
     * @return the view class names
     */
    public Set<String> getViewClassNames() {
        return viewClassNames;
    }

    /**
     * Add a dependency to this component.  If the same dependency is added multiple times, only the first will
     * take effect.
     *
     * @param serviceName the service name of the dependency
     * @param type the type of the dependency (required or optional)
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
     * Create the component configuration which will be used to construct the component instance.
     *
     * @param phaseContext the deployment phase context
     * @param componentClass the component class
     * @return the component configuration
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException if an error occurs
     */
    public final AbstractComponentConfiguration createComponentConfiguration(final DeploymentPhaseContext phaseContext, final Class<?> componentClass) throws DeploymentUnitProcessingException {
        AbstractComponentConfiguration configuration = constructComponentConfiguration();
        configuration.setComponentClass(componentClass);
        prepareComponentConfiguration(configuration, phaseContext);
        return configuration;
    }

    /**
     * Construct the component configuration instance.
     *
     * @return the component configuration instance
     */
    protected abstract AbstractComponentConfiguration constructComponentConfiguration();

    /**
     * Set up the component configuration from this description.  Overriding methods should call up to the superclass
     * method at the start of processing.
     *
     * @param configuration the configuration to prepare
     * @param phaseContext the phase context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException if an error occurs
     */
    protected void prepareComponentConfiguration(AbstractComponentConfiguration configuration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module= deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        // Create the table of component class methods
        final Map<MethodIdentifier, Method> componentMethods = new HashMap<MethodIdentifier, Method>();
        final Map<Method, InterceptorFactory> componentToInterceptorFactory = new IdentityHashMap<Method, InterceptorFactory>();
        Class<?> componentClass = configuration.getComponentClass();
        Class<?> currentClass = componentClass;
        do {
            final ClassReflectionIndex<?> classIndex = index.getClassIndex(currentClass);
            // Mapping of method identifiers to component (target) methods
            // Mapping of component methods to corresponding instance interceptor factories
            for (Method componentMethod : classIndex.getMethods()) {
                final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(componentMethod);
                int modifiers = componentMethod.getModifiers();
                if (! Modifier.isStatic(modifiers) && ! Modifier.isFinal(modifiers)) {
                    componentMethods.put(MethodIdentifier.getIdentifierForMethod(componentMethod), componentMethod);
                    // assemble the final set of interceptor factories for this method.
                    final List<InterceptorFactory> interceptorFactories = new ArrayList<InterceptorFactory>();
                    // TODO: default-level interceptors if applicable
                    // TODO: This code should be somewhere else
                    //Now we need to create all our interceptors
                    //this is probably not the right place for it long term
                    //and we need to look at Exclude(Class/Default)Interceptors
                    //and deployment descriptor overrides
                    //and all the interceptor config probably should have its own class
                    //and probably more stuff as well

                    //first class level interceptor
                    if(!methodExcludeClassInterceptors.contains(methodIdentifier)){
                        for(final InterceptorDescription interceptor: classInterceptors) {
                            registerComponentInterceptor(interceptor, configuration, module, index, interceptorFactories);
                        }
                    }
                    //now method level interceptors
                    List<InterceptorDescription> methodLevelInterceptors = methodInterceptors.get(methodIdentifier);
                    if(methodLevelInterceptors != null)
                        for(final InterceptorDescription interceptor : methodLevelInterceptors) {
                            registerComponentInterceptor(interceptor, configuration, module, index, interceptorFactories);
                        }
                    //now register around invoke methods on the bean and its superclasses
                    //this is a linked hash set so methods will be invoked in the correct order
                    for(Map.Entry<String, InterceptorMethodDescription> entry : aroundInvokeMethods.entrySet()) {
                        try {
                            final InterceptorMethodDescription aroundInvoke = entry.getValue();
                            final Class<?> methodDeclaringClass = module.getClassLoader().loadClass(entry.getKey());
                            final ClassReflectionIndex<?> methodDeclaringClassIndex = index.getClassIndex(methodDeclaringClass);
                            //we know what the signature is
                            final Method aroundInvokeMethod = methodDeclaringClassIndex.getMethod(Object.class, aroundInvoke.getIdentifier().getName(), InvocationContext.class);
                            interceptorFactories.add(new MethodInterceptorFactory(AbstractComponent.INSTANCE_FACTORY, aroundInvokeMethod));
                        } catch(ClassNotFoundException e){
                            //this should never happen
                            throw new DeploymentUnitProcessingException("Failed to load interceptor class " + entry.getKey());
                        }
                    }

                    // The final interceptor invokes the method on the associated instance
                    interceptorFactories.add(new MethodInvokingInterceptorFactory(AbstractComponent.INSTANCE_FACTORY, componentMethod));
                    componentToInterceptorFactory.put(componentMethod, Interceptors.getChainedInterceptorFactory(interceptorFactories));
                    processComponentMethod(configuration, componentMethod);
                }
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);

        //populate lifecycle method information
        LifecycleInterceptorBuilder.createLifecycleInterceptors(this,configuration,module,index);

        // Now create the views
        // Mapping of view methods to corresponding instance interceptor factories
        final Map<Class<?>, ProxyFactory<?>> proxyFactories = configuration.getProxyFactories();
        final Map<Method, InterceptorFactory> viewToInterceptorFactory = configuration.getInterceptorFactoryMap();
        for (String viewClassName : viewClassNames) {
            final Class<?> viewClass;
            try {
                viewClass = Class.forName(viewClassName, false, componentClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load view class " + viewClassName, e);
            }
            final ProxyFactory<?> factory = getProxyFactory(viewClass);
            proxyFactories.put(viewClass, factory);
            // acquire the complete list of possible invoked methods for this view
            final List<Method> methods = new ArrayList<Method>();
            for (Method viewMethod : factory.getCachedMethods()) {
                methods.add(viewMethod);
                Method componentMethod = componentMethods.get(MethodIdentifier.getIdentifierForMethod(viewMethod));
                // todo - it's probably an error if the view has more methods than the component
                if (componentMethod != null) {
                    // Create the mapping of this view method to the interceptor factory for the target method
                    viewToInterceptorFactory.put(viewMethod, componentToInterceptorFactory.get(componentMethod));
                }
                processViewMethod(configuration, viewClass, viewMethod, componentMethod);
            }
        }

        // Now add dependencies
        final Map<ServiceName, InjectedValue<Object>> dependencyInjections = configuration.getDependencyInjections();
        for (Map.Entry<ServiceName, ServiceBuilder.DependencyType> entry : dependencies.entrySet()) {
            InjectedValue<Object> value = new InjectedValue<Object>();
            dependencyInjections.put(entry.getKey(), value);
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

    protected void processComponentMethod(AbstractComponentConfiguration configuration, Method componentMethod) {
        // do nothing
    }

    protected void processViewMethod(AbstractComponentConfiguration configuration, Class<?> viewClass, Method viewMethod, Method componentMethod) {
        // do nothing
    }

    private void registerComponentInterceptor(InterceptorDescription interceptor, AbstractComponentConfiguration configuration, Module module, DeploymentReflectionIndex index, List<InterceptorFactory> interceptorFactories) throws DeploymentUnitProcessingException {
        try {
            final Class<?> interceptorClass = module.getClassLoader().loadClass(interceptor.getInterceptorClassName());
            //we don't actually have the required resource injections at this stage
            //so we pass in the configuration, as it will have been populated with resource injections by
            //the time the factory is called.

            final InjectingInterceptorInstanceFactory instanceFactory = new InjectingInterceptorInstanceFactory(new SimpleInterceptorInstanceFactory(interceptorClass),interceptorClass,configuration);
            //we need to create an Interceptor for every around invoke method
            for(InterceptorMethodDescription aroundInvoke : interceptor.getAroundInvokeMethods()) {
                final Class<?> methodDeclaringClass = module.getClassLoader().loadClass(aroundInvoke.getDeclaringClass());
                final ClassReflectionIndex<?> methodDeclaringClassIndex = index.getClassIndex(methodDeclaringClass);
                //we know what the signature is
                final Method aroundInvokeMethod = methodDeclaringClassIndex.getMethod(Object.class, aroundInvoke.getIdentifier().getName(), InvocationContext.class);
                interceptorFactories.add(new MethodInterceptorFactory(instanceFactory, aroundInvokeMethod));
            }
        } catch(ClassNotFoundException e){
            throw new DeploymentUnitProcessingException("Failed to load interceptor class " + interceptor.getInterceptorClassName());
        }
    }

    private static final AtomicInteger seq = new AtomicInteger();

    private static <T> ProxyFactory<?> getProxyFactory(Class<T> type) {
        String proxyName = type.getName() + "$$ee$proxy" + seq.getAndIncrement();
        if (type.isInterface()) {
            return new ProxyFactory<Object>(proxyName, Object.class, type.getClassLoader(), type);
        } else {
            return new ProxyFactory<T>(proxyName, type, type.getClassLoader());
        }
    }
}
