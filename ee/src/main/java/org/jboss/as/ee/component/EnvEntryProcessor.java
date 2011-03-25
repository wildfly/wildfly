/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.modules.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Deployment processor that sets up env-entry bindings
 *
 * @author Stuart Douglas
 */
public class EnvEntryProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEModuleDescription description = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(description == null) {
            return;
        }
        if(environment != null) {
            final List<BindingDescription> bindings = getEnvironmentEntries(environment, module.getClassLoader(), deploymentReflectionIndex);
            description.getBindingsContainer().addBindings(bindings);
        }
        for(final AbstractComponentDescription componentDescription : description.getComponentDescriptions()) {
            if(componentDescription.getDeploymentDescriptorEnvironment() != null) {
                final List<BindingDescription> bindings = getEnvironmentEntries(componentDescription.getDeploymentDescriptorEnvironment(), module.getClassLoader(), deploymentReflectionIndex);
                componentDescription.addBindings(bindings);
            }
        }
    }


    private List<BindingDescription> getEnvironmentEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        List<BindingDescription> bindings = new ArrayList<BindingDescription>();
        final EnvironmentEntriesMetaData entries = environment.getEnvironment().getEnvironmentEntries();
        if(entries == null) {
            return bindings;
        }

        for(EnvironmentEntryMetaData envEntry : entries) {
            final String name;
            if(envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getEnvEntryName();
            }
            BindingDescription description  = new BindingDescription(name);

            Class<?> classType = null;
            if(envEntry.getType() != null) {
                try {
                    classType = classLoader.loadClass(envEntry.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + envEntry.getType() + " referenced in env-entry ",e);
                }
            }

            description.setDependency(true);

            classType = processInjectionTargets(classLoader, deploymentReflectionIndex, envEntry, description, classType);

            final String value = envEntry.getValue();
            final String type = classType.getName();
            description.setBindingType(type);

            if(type.equals(String.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(value));
            } else if(type.equals(Integer.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Integer.valueOf(value)));
            } else if(type.equals(Short.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Short.valueOf(value)));
            } else if(type.equals(Long.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Long.valueOf(value)));
            } else if(type.equals(Byte.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Byte.valueOf(value)));
            }  else if(type.equals(Double.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Double.valueOf(value)));
            } else if(type.equals(Float.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Float.valueOf(value)));
            } else if(type.equals(Boolean.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Boolean.valueOf(value)));
            } else if(type.equals(Character.class.getName())) {
                if(value.length() != 1) {
                    throw new DeploymentUnitProcessingException("env-entry of type java.lang.Character is not exactly one character long " + value);
                }
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(value.charAt(0)));
            } else if(type.equals(Class.class.getName())) {
                try {
                    description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(classLoader.loadClass(value)));
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load class " + value + " specified in env-entry");
                }
            }  else if(classType.isEnum() || (classType.getEnclosingClass() != null && classType.getEnclosingClass().isEnum())) {
                description.setReferenceSourceDescription(new EnvEntryReferenceSourceDescription(Enum.valueOf((Class)classType,value)));
            } else {
                throw new DeploymentUnitProcessingException("Unkown env-entry type " + type);
            }
            bindings.add(description);
        }
        return bindings;
    }


    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
