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

import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleDescription {
    private final String applicationName;
    private volatile String moduleName;
    private final Map<String, ComponentDescription> componentsByName = new HashMap<String, ComponentDescription>();
    private final Map<String, List<ComponentDescription>> componentsByClassName = new HashMap<String, List<ComponentDescription>>();
    /**
     * Resource injections that only get installed if a binding is set up
     * See EE 5.4.1.3
     */
    private final Map<String, List<LazyResourceInjection>> lazyResourceInjections = new HashMap<String, List<LazyResourceInjection>>();

    private InjectedEENamespaceContextSelector namespaceContextSelector;

    private final Deque<EEModuleConfigurator> moduleConfigurators = new ArrayDeque<EEModuleConfigurator>();

    /**
     * Construct a new instance.
     *
     * @param applicationName the application name
     * @param moduleName      the module name
     */
    public EEModuleDescription(final String applicationName, final String moduleName) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
    }

    public void addLazyResourceInjection(LazyResourceInjection injection) {
        //TODO: lazy binding and comp/module aliasing is not really compatible
        String name = injection.getLocalContextName();
        //we store all the bindings as absolute bindings
        if (!name.startsWith("java:")) {
            //there is the potential for both java:comp and java:module bindings to satisfy these injections
            List<LazyResourceInjection> list = lazyResourceInjections.get("java:comp/env/" + name);
            if (list == null) {
                lazyResourceInjections.put("java:comp/env/" + name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
            list = lazyResourceInjections.get("java:module/env/" + name);
            if (list == null) {
                lazyResourceInjections.put("java:module/env/" + name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
        } else {
            List<LazyResourceInjection> list = lazyResourceInjections.get(name);
            if (list == null) {
                lazyResourceInjections.put(name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
        }
    }

    public Map<String, List<LazyResourceInjection>> getLazyResourceInjections() {
        return lazyResourceInjections;
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
            throw new IllegalArgumentException("componentName is null");
        }
        if (componentClassName == null) {
            throw new IllegalArgumentException("componentClassName is null");
        }
        if (componentsByName.containsKey(componentName)) {
            throw new IllegalArgumentException("A component named '" + componentName + "' is already defined in this module");
        }
        componentsByName.put(componentName, description);
        List<ComponentDescription> list = componentsByClassName.get(componentClassName);
        if(list == null) {
            componentsByClassName.put(componentClassName, list = new ArrayList<ComponentDescription>(1));
        }
        list.add(description);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean hasComponent(final String name) {
        return componentsByName.containsKey(name);
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

    public Deque<EEModuleConfigurator> getConfigurators() {
        return this.moduleConfigurators;
    }

    public InjectedEENamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelector;
    }

    public void setNamespaceContextSelector(InjectedEENamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }
}
