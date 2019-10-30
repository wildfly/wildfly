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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleDescription implements ResourceInjectionTarget {
    private final String applicationName;
    private volatile String moduleName;
    private final String earApplicationName;
    //distinct name defaults to the empty string
    private volatile String distinctName = "";
    private final Map<String, ComponentDescription> componentsByName = new HashMap<String, ComponentDescription>();
    private final Map<String, List<ComponentDescription>> componentsByClassName = new HashMap<String, List<ComponentDescription>>();
    private final Map<String, EEModuleClassDescription> classDescriptions = new HashMap<String, EEModuleClassDescription>();
    private final Map<String, InterceptorClassDescription> interceptorClassOverrides = new HashMap<String, InterceptorClassDescription>();

    /**
     * Additional interceptor environment that was defined in the deployment descriptor <interceptors/> element.
     */
    private final Map<String, InterceptorEnvironment> interceptorEnvironment = new HashMap<String, InterceptorEnvironment>();

    /**
     * A map of message destinations names to their resolved JNDI name
     */
    private final Map<String, String> messageDestinations = new HashMap<String, String>();

    private InjectedEENamespaceContextSelector namespaceContextSelector;

    // Module Bindings
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
    //injections that have been set in the components deployment descriptor
    private final Map<String, Map<InjectionTarget, ResourceInjectionConfiguration>> resourceInjections = new HashMap<String, Map<InjectionTarget, ResourceInjectionConfiguration>>();

    private final boolean appClient;

    private ServiceName defaultClassIntrospectorServiceName = ReflectiveClassIntrospector.SERVICE_NAME;

    private final ConcurrentContext concurrentContext;

    private final EEDefaultResourceJndiNames defaultResourceJndiNames;

    /**
     * The default security domain for the module.
     */
    private String defaultSecurityDomain;

    /**
     * The number of registered startup beans.
     */
    private int startupBeansCount;

    /**
     * Construct a new instance.
     *
     * @param applicationName    the application name (which is same as the module name if the .ear is absent)
     * @param moduleName         the module name
     * @param earApplicationName The application name (which is null if the .ear is absent)
     * @param appClient          indicates if the process type is an app client
     */
    public EEModuleDescription(final String applicationName, final String moduleName, final String earApplicationName, final boolean appClient) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.earApplicationName = earApplicationName;
        this.appClient = appClient;
        this.concurrentContext = new ConcurrentContext();
        this.defaultResourceJndiNames = new EEDefaultResourceJndiNames();
    }

    /**
     * Adds or retrieves an existing EEModuleClassDescription for the local module. This method should only be used
     * for classes that reside within the current deployment unit, usually by annotation scanners that are attaching annotation
     * information.
     * <p/>
     * This
     *
     * @param className The class name
     * @return The new or existing {@link EEModuleClassDescription}
     */
    public EEModuleClassDescription addOrGetLocalClassDescription(final String className) {
        if (className == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("className", "module", moduleName);
        }
        EEModuleClassDescription ret = classDescriptions.get(className);
        if (ret == null) {
            classDescriptions.put(className, ret = new EEModuleClassDescription(className));
        }
        return ret;
    }

    /**
     * Returns a class that is local to this module
     *
     * @param className The class
     * @return The description, or null if not found
     */
    EEModuleClassDescription getClassDescription(final String className) {
        return classDescriptions.get(className);
    }

    /**
     * Returns all class descriptions in this module
     *
     * @return All class descriptions
     */
    public Collection<EEModuleClassDescription> getClassDescriptions() {
        return classDescriptions.values();
    }

    public ServiceName getDefaultClassIntrospectorServiceName() {
        return defaultClassIntrospectorServiceName;
    }

    public void setDefaultClassIntrospectorServiceName(ServiceName defaultClassIntrospectorServiceName) {
        this.defaultClassIntrospectorServiceName = defaultClassIntrospectorServiceName;
    }

    /**
     * Add a component to this module.
     *
     * @param description the component description
     */
    public void addComponent(ComponentDescription description) {
        final String componentName = description.getComponentName();
        final String componentClassName = description.getComponentClassName();
        if (componentName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("componentName", "module", moduleName);
        }
        if (componentClassName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("componentClassName","module", moduleName);
        }
        if (componentsByName.containsKey(componentName)) {
            throw EeLogger.ROOT_LOGGER.componentAlreadyDefined(componentName);
        }
        componentsByName.put(componentName, description);
        List<ComponentDescription> list = componentsByClassName.get(componentClassName);
        if (list == null) {
            componentsByClassName.put(componentClassName, list = new ArrayList<ComponentDescription>(1));
        }
        list.add(description);
    }

    public void removeComponent(final String componentName, final String componentClassName) {
        componentsByName.remove(componentName);
        componentsByClassName.remove(componentClassName);
    }

    /**
     * Returns the application name which can be the same as the module name, in the absence of a .ear top level
     * deployment
     *
     * @return
     * @see {@link #getEarApplicationName()}
     */
    public String getApplicationName() {
        //if no application name is set just return the module name
        //this means that if the module name is changed the application name
        //will change as well
        if(applicationName == null) {
            return moduleName;
        }
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean hasComponent(final String name) {
        return componentsByName.containsKey(name);
    }

    /**
     * @return true if the process type is an app client
     */
    public boolean isAppClient() {
        return appClient;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public ComponentDescription getComponentByName(String name) {
        return componentsByName.get(name);
    }

    public List<ComponentDescription> getComponentsByClassName(String className) {
        final List<ComponentDescription> ret = componentsByClassName.get(className);
        return ret == null ? Collections.<ComponentDescription>emptyList() : ret;
    }

    public Collection<ComponentDescription> getComponentDescriptions() {
        return componentsByName.values();
    }

    public InjectedEENamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelector;
    }

    public void setNamespaceContextSelector(InjectedEENamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    public String getDistinctName() {
        return distinctName;
    }

    public void setDistinctName(String distinctName) {
        if (distinctName == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("distinctName", "module", moduleName);
        }
        this.distinctName = distinctName;
    }

    /**
     * Unlike the {@link #getApplicationName()} which follows the Java EE6 spec semantics i.e. application name is the
     * name of the top level deployment (even if it is just a jar and not an ear), this method returns the
     * application name which follows the EJB spec semantics i.e. the application name is the
     * .ear name or any configured value in application.xml. This method returns null in the absence of a .ear
     *
     * @return
     */
    public String getEarApplicationName() {
        return this.earApplicationName;
    }

    /**
     * Get module level interceptor method overrides that are set up in ejb-jar.xml
     *
     * @param className The class name
     * @return The overrides, or null if no overrides have been set up
     */
    public InterceptorClassDescription getInterceptorClassOverride(final String className) {
        return interceptorClassOverrides.get(className);
    }

    /**
     * Adds a module level interceptor class override, it is merged with any existing overrides if they exist
     *
     * @param className The class name
     * @param override  The override
     */
    public void addInterceptorMethodOverride(final String className, final InterceptorClassDescription override) {
        interceptorClassOverrides.put(className, InterceptorClassDescription.merge(interceptorClassOverrides.get(className), override));
    }

    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
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

    public void addMessageDestination(final String name, final String jndiName) {
        messageDestinations.put(name, jndiName);
    }

    public Map<String, String> getMessageDestinations() {
        return Collections.unmodifiableMap(messageDestinations);
    }

    public void addInterceptorEnvironment(final String interceptorClassName, final InterceptorEnvironment env) {
        interceptorEnvironment.put(interceptorClassName, env);
    }

    public Map<String, InterceptorEnvironment> getInterceptorEnvironment() {
        return interceptorEnvironment;
    }

    public ConcurrentContext getConcurrentContext() {
        return concurrentContext;
    }

    public EEDefaultResourceJndiNames getDefaultResourceJndiNames() {
        return defaultResourceJndiNames;
    }

    public String getDefaultSecurityDomain() {
        return defaultSecurityDomain;
    }

    public void setDefaultSecurityDomain(String defaultSecurityDomain) {
        this.defaultSecurityDomain = defaultSecurityDomain;
    }

    public int getStartupBeansCount() {
        return this.startupBeansCount;
    }

    public int registerStartupBean() {
        return ++this.startupBeansCount;
    }
}
