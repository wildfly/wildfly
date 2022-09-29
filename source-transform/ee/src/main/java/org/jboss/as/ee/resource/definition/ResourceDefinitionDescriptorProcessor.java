/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.resource.definition;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Foundation for resource definition deployment descriptor processors.
 *
 * @author Eduardo Martins
 */
public abstract class ResourceDefinitionDescriptorProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(final DeploymentUnit deploymentUnit, final DeploymentDescriptorEnvironment environment, final ResourceInjectionTarget resourceInjectionTarget, final ComponentDescription componentDescription, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        final ResourceDefinitionInjectionSources injectionSources = new ResourceDefinitionInjectionSources();
        processEnvironment(environment.getEnvironment(), injectionSources);
        if (injectionSources.bindingConfigurations != null) {
            return injectionSources.bindingConfigurations;
        } else {
            return Collections.emptyList();
        }
    }

    protected abstract void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException;

    public static class ResourceDefinitionInjectionSources {

        private List<BindingConfiguration> bindingConfigurations;

        public void addResourceDefinitionInjectionSource(ResourceDefinitionInjectionSource injectionSource) {
            if (bindingConfigurations == null) {
                bindingConfigurations = new ArrayList<>();
            }
            bindingConfigurations.add(new BindingConfiguration(injectionSource.getJndiName(), injectionSource));
        }
    }

}
