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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * A description of a generic Java EE component.  The description is pre-classloading so it references everything by name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ComponentDescription implements ResourceInjectionTarget {

    private static final DefaultComponentConfigurator DEFAULT_COMPONENT_CONFIGURATOR = new DefaultComponentConfigurator();
    private static final DefaultInterceptorConfigurator DEFAULT_INTERCEPTOR_CONFIGURATOR = new DefaultInterceptorConfigurator();
    private static final DefaultComponentViewConfigurator DEFAULT_COMPONENT_VIEW_CONFIGURATOR = new DefaultComponentViewConfigurator();

    private final ServiceName serviceName;
    private ServiceName contextServiceName;
    private final String componentName;
    private final String componentClassName;
    private final EEModuleDescription moduleDescription;
    private final Set<ViewDescription> views = new HashSet<ViewDescription>();

    /**
     * Interceptors methods that have been defined by the deployment descriptor
     */
    private final Map<String, InterceptorClassDescription> interceptorClassOverrides = new HashMap<String, InterceptorClassDescription>();

    private List<InterceptorDescription> classInterceptors = new ArrayList<InterceptorDescription>();
    private List<InterceptorDescription> defaultInterceptors = new ArrayList<InterceptorDescription>();
    private final Map<MethodIdentifier, List<InterceptorDescription>> methodInterceptors = new HashMap<MethodIdentifier, List<InterceptorDescription>>();
    private final Set<MethodIdentifier> methodExcludeDefaultInterceptors = new HashSet<MethodIdentifier>();
    private final Set<MethodIdentifier> methodExcludeClassInterceptors = new HashSet<MethodIdentifier>();
    private Set<InterceptorDescription> allInterceptors;
    private boolean excludeDefaultInterceptors = false;
    private boolean ignoreLifecycleInterceptors = false;

    private final Set<ServiceName> dependencies = new HashSet<ServiceName>();

    private ComponentNamingMode namingMode = ComponentNamingMode.USE_MODULE;

    private DeploymentDescriptorEnvironment deploymentDescriptorEnvironment;


    // Bindings
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
    //injections that have been set in the components deployment descriptor
    private final Map<String, Map<InjectionTarget, ResourceInjectionConfiguration>> resourceInjections = new HashMap<String, Map<InjectionTarget, ResourceInjectionConfiguration>>();

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
     * @param deploymentUnitServiceName the service name of the DU containing this component
     */
    public ComponentDescription(final String componentName, final String componentClassName, final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName) {
        this.moduleDescription = moduleDescription;
        if (componentName == null) {
            throw EeLogger.ROOT_LOGGER.nullName("component");
        }
        if (componentClassName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("componentClassName", "component", componentName);
        }
        if (moduleDescription == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("moduleDescription", "component", componentName);
        }
        if (deploymentUnitServiceName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("deploymentUnitServiceName", "component", componentName);
        }
        serviceName = BasicComponent.serviceNameOf(deploymentUnitServiceName, componentName);
        this.componentName = componentName;
        this.componentClassName = componentClassName;
        configurators.addLast(DEFAULT_COMPONENT_CONFIGURATOR);
        configurators.addLast(DEFAULT_INTERCEPTOR_CONFIGURATOR);
        configurators.addLast(DEFAULT_COMPONENT_VIEW_CONFIGURATOR);
    }

    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        return new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
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
     * Set context service name.
     *
     * @param contextServiceName
     */
    public void setContextServiceName(final ServiceName contextServiceName) {
        this.contextServiceName = contextServiceName;
    }

    /**
     * Get the context service name.
     *
     * @return the context service name
     */
    public ServiceName getContextServiceName() {
        if (contextServiceName != null) return contextServiceName;
        if (getNamingMode() == ComponentNamingMode.CREATE) {
            return ContextNames.contextServiceNameOfComponent(getApplicationName(), getModuleName(), getComponentName());
        } else if (getNamingMode() == ComponentNamingMode.USE_MODULE) {
            return ContextNames.contextServiceNameOfModule(getApplicationName(), getModuleName());
        } else {
            throw new IllegalStateException();
        }
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
     * Get the service name of this components start service
     *
     * @return The start service name
     */
    public ServiceName getStartServiceName() {
        return serviceName.append("START");
    }

    /**
     * Get the service name of this components create service
     *
     * @return The create service name
     */
    public ServiceName getCreateServiceName() {
        return serviceName.append("CREATE");
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

    public boolean isIgnoreLifecycleInterceptors() {
        return ignoreLifecycleInterceptors;
    }

    /**
     * If this component should ignore lifecycle interceptors. This should generally only be set when they are going
     * to be handled by an external framework such as Weld.
     *
     */
    public void setIgnoreLifecycleInterceptors(boolean ignoreLifecycleInterceptors) {
        this.ignoreLifecycleInterceptors = ignoreLifecycleInterceptors;
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
     */
    public void addClassInterceptor(InterceptorDescription description) {
        classInterceptors.add(description);
        this.allInterceptors = null;
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
     */
    public void addMethodInterceptor(MethodIdentifier method, InterceptorDescription description) {
        //we do not add method level interceptors to the set of interceptor classes,
        //as their around invoke annotations
        List<InterceptorDescription> interceptors = methodInterceptors.get(method);
        if (interceptors == null) {
            methodInterceptors.put(method, interceptors = new ArrayList<InterceptorDescription>());
        }
        final String name = description.getInterceptorClassName();
        // add the interceptor class to the EEModuleDescription
        interceptors.add(description);
        this.allInterceptors = null;
    }

    /**
     * Sets the method level interceptors for a method, and marks it as exclude class and default level interceptors.
     * <p/>
     * This is used to set the final interceptor order after it has been modifier by the deployment descriptor
     *
     * @param identifier              the method identifier
     * @param interceptorDescriptions The interceptors
     */
    public void setMethodInterceptors(MethodIdentifier identifier, List<InterceptorDescription> interceptorDescriptions) {
        methodInterceptors.put(identifier, interceptorDescriptions);
        methodExcludeClassInterceptors.add(identifier);
        methodExcludeDefaultInterceptors.add(identifier);
    }

    /**
     * Adds an interceptor class method override, merging it with existing overrides (if any)
     *
     * @param className The class name
     * @param override  The method override
     */
    public void addInterceptorMethodOverride(final String className, final InterceptorClassDescription override) {
        interceptorClassOverrides.put(className, InterceptorClassDescription.merge(interceptorClassOverrides.get(className), override));
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
            throw EeLogger.ROOT_LOGGER.nullVar("namingMode", "component", componentName);
        }
        this.namingMode = namingMode;
    }

    /**
     * @return The module description for the component
     */
    public EEModuleDescription getModuleDescription() {
        return moduleDescription;
    }

    /**
     * Add a dependency to this component.  If the same dependency is added multiple times, only the first will
     * take effect.
     *
     * @param serviceName the service name of the dependency
     */
    public void addDependency(ServiceName serviceName) {
        if (serviceName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("serviceName", "component", componentName);
        }
        dependencies.add(serviceName);
    }

    /**
     * Get the dependency set.
     *
     * @return the dependency set
     */
    public Set<ServiceName> getDependencies() {
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
     *
     *
     * @return true If this component type is eligible for a timer service
     */
    public boolean isTimerServiceApplicable() {
        return false;
    }

    /**
     *
     * @return <code>true</code> if this component has timeout methods and is eligible for a 'real' timer service
     */
    public boolean isTimerServiceRequired() {
        return isTimerServiceApplicable() && !getTimerMethods().isEmpty();
    }

    /**
     *
     * @return The set of all method identifiers for the timeout methods
     */
    public Set<MethodIdentifier> getTimerMethods() {
        return Collections.emptySet();
    }

    public boolean isPassivationApplicable() {
        return false;
    }

    /**
     * Get the configurators for this component.
     *
     * @return the configurators
     */
    public Deque<ComponentConfigurator> getConfigurators() {
        return configurators;
    }

    public boolean isIntercepted() {
        return true;
    }

    /**
     * @return <code>true</code> if errors should be ignored when installing this component
     */
    public boolean isOptional() {
        return false;
    }

    static class InjectedConfigurator implements DependencyConfigurator<ComponentStartService> {

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

        public void configureDependency(final ServiceBuilder<?> serviceBuilder, ComponentStartService service) throws DeploymentUnitProcessingException {
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

    public void addResourceInjection(final ResourceInjectionConfiguration injection) {
        String className = injection.getTarget().getClassName();
        Map<InjectionTarget, ResourceInjectionConfiguration> map = resourceInjections.get(className);
        if (map == null) {
            resourceInjections.put(className, map = new HashMap<InjectionTarget, ResourceInjectionConfiguration>());
        }
        map.put(injection.getTarget(), injection);
    }

    public Map<InjectionTarget, ResourceInjectionConfiguration> getResourceInjections(final String className) {
        Map<InjectionTarget, ResourceInjectionConfiguration> injections = resourceInjections.get(className);
        if (injections == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap(injections);
        }
    }

    /**
     * If this method returns true then Weld will directly create the component instance,
     * which will apply interceptors and decorators via sub classing.
     *
     * For most components this is not necessary.
     *
     * Also not that even though EJB's are intercepted, their interceptor is done through
     * a different method that integrates with the existing EJB interceptor chain
     *
     */
    public boolean isCDIInterceptorEnabled() {
        return false;
    }

    public static InterceptorClassDescription mergeInterceptorConfig(final Class<?> clazz, final EEModuleClassDescription classDescription, final ComponentDescription description, final boolean metadataComplete) {
        final InterceptorClassDescription interceptorConfig;
        if (classDescription != null && !metadataComplete) {
            interceptorConfig = InterceptorClassDescription.merge(classDescription.getInterceptorClassDescription(), description.interceptorClassOverrides.get(clazz.getName()));
        } else {
            interceptorConfig = InterceptorClassDescription.merge(null, description.interceptorClassOverrides.get(clazz.getName()));
        }
        return interceptorConfig;
    }

}
