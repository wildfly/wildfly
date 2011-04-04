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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

import static org.jboss.as.server.deployment.Attachments.MODULE;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleConfiguration {
    private final String applicationName;
    private final String moduleName;
    private final Map<String, EEModuleClassConfiguration> classesByName;
    private final Map<String, ComponentConfiguration> componentsByName;
    private final Map<String, ComponentConfiguration> componentsByClassName;

    EEModuleConfiguration(EEModuleDescription description, DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(MODULE);
        ModuleClassLoader classLoader = module.getClassLoader();
        applicationName = description.getApplicationName();
        moduleName = description.getModuleName();
        final Map<String, EEModuleClassConfiguration> classesByName = new HashMap<String, EEModuleClassConfiguration>();
        for (EEModuleClassDescription classDescription : description.getClassDescriptions()) {
            String className = classDescription.getClassName();
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load class " + className, e);
            }
            EEModuleClassConfiguration classConfiguration = new EEModuleClassConfiguration(clazz, this);
            for (ClassConfigurator configurator : classDescription.getConfigurators()) {
                configurator.configure(context, classDescription, classConfiguration);
            }
            classesByName.put(className, classConfiguration);
        }
        final Map<String, ComponentConfiguration> componentsByName = new HashMap<String, ComponentConfiguration>();
        final Map<String, ComponentConfiguration> componentsByClassName = new HashMap<String, ComponentConfiguration>();
        for (ComponentDescription componentDescription : description.getComponentDescriptions()) {
            ComponentConfiguration componentConfiguration = new ComponentConfiguration(componentDescription, classesByName.get(componentDescription.getComponentClassName()));
            for (ComponentConfigurator configurator : componentDescription.getConfigurators()) {
                configurator.configure(context, componentDescription, componentConfiguration);
            }
        }
        this.classesByName = classesByName;
        this.componentsByName = componentsByName;
        this.componentsByClassName = componentsByClassName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public EEModuleClassConfiguration getClassConfiguration(String className) {
        return classesByName.get(className);
    }

    public ComponentConfiguration getComponentConfiguration(String componentName) {
        return componentsByName.get(componentName);
    }

    public ComponentConfiguration getComponentConfigurationByClassName(String className) {
        return componentsByClassName.get(className);
    }

    public Collection<ComponentConfiguration> getComponentConfigurations() {
        return componentsByName.values();
    }
}
