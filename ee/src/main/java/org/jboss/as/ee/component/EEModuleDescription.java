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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleDescription {
    private final String appName;
    private final String moduleName;
    private final Map<String, AbstractComponentDescription> componentsByName = new HashMap<String, AbstractComponentDescription>();
    private final Map<String, AbstractComponentDescription> componentsByClassName = new HashMap<String, AbstractComponentDescription>();
    private final List<InjectionFactory> injectionFactories = new ArrayList<InjectionFactory>();
    private final Map<String, Set<AbstractComponentDescription>> componentsByViewName = new HashMap<String, Set<AbstractComponentDescription>>();

    /**
     * Construct a new instance.
     *
     * @param appName the application name
     * @param moduleName the module name
     */
    public EEModuleDescription(final String appName, final String moduleName) {
        this.appName = appName;
        this.moduleName = moduleName;
    }

    /**
     * Add a component to this module.
     *
     * @param description the component description
     */
    public void addComponent(AbstractComponentDescription description) {
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
        if (componentsByClassName.containsKey(componentClassName)) {
            throw new IllegalArgumentException("A component of class " + componentClassName + " is already defined in this module");
        }
        componentsByName.put(componentName, description);
        componentsByClassName.put(componentClassName, description);
        for(String viewName : description.getViewClassNames()) {
            Set<AbstractComponentDescription> viewComponents = componentsByViewName.get(viewName);
            if(viewComponents == null) {
                viewComponents = new HashSet<AbstractComponentDescription>();
                componentsByViewName.put(viewName, viewComponents);
            }
            viewComponents.add(description);
        }
    }

    public String getAppName() {
        return appName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public AbstractComponentDescription getComponentByName(String name) {
        return componentsByName.get(name);
    }

    public AbstractComponentDescription getComponentByClassName(String className) {
        return componentsByClassName.get(className);
    }

    public Collection<AbstractComponentDescription> getComponentDescriptions() {
        return componentsByName.values();
    }

    public void addInjectionFactory(InjectionFactory factory) {
        injectionFactories.add(factory);
    }

    public List<InjectionFactory> getInjectionFactories() {
        return Collections.unmodifiableList(injectionFactories);
    }

    public Map<String, Set<AbstractComponentDescription>> getComponentsByViewName() {
        return Collections.unmodifiableMap(componentsByViewName);
    }

    public Set<AbstractComponentDescription> getComponentsForViewName(final String name) {
        return componentsByViewName.get(name);
    }
}
