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
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleConfiguration {
    private final String applicationName;
    private final String moduleName;
    private final List<ComponentConfiguration> componentConfigurations;

    // Module Bindings
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();

    EEModuleConfiguration(EEModuleDescription description, DeploymentPhaseContext context, final Module module) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        ModuleClassLoader classLoader = module.getClassLoader();
        applicationName = description.getApplicationName();
        moduleName = description.getModuleName();

        // run this configuration through the module configurators
        for (EEModuleConfigurator configurator : description.getConfigurators()) {
            configurator.configure(context, description, this);
        }

        this.componentConfigurations = new ArrayList<ComponentConfiguration>();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Collection<ComponentConfiguration> getComponentConfigurations() {
        return Collections.unmodifiableList(componentConfigurations);
    }

    public void addComponentConfiguration(ComponentConfiguration configuration) {
        componentConfigurations.add(configuration);
    }

    public List<BindingConfiguration> getBindingConfigurations() {
        return this.bindingConfigurations;
    }
}
