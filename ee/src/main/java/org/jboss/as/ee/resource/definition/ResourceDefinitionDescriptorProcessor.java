/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
