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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.ResourceEnvironmentReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceEnvironmentReferencesMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * Deployment processor that sets up env-entry bindings
 *
 * @author Stuart Douglas
 */
public class EnvEntryProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingDescription> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, EEModuleDescription moduleDescription, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        List<BindingDescription> bindings = new ArrayList<BindingDescription>();
        bindings.addAll(getEnvironmentEntries(environment, classLoader, deploymentReflectionIndex));
        bindings.addAll(getResourceEnvRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription));
        bindings.addAll(getResourceRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription));
        return bindings;
    }

    private List<BindingDescription> getResourceEnvRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        List<BindingDescription> bindings = new ArrayList<BindingDescription>();
        final ResourceEnvironmentReferencesMetaData entries = environment.getEnvironment().getResourceEnvironmentReferences();
        if(entries == null) {
            return bindings;
        }
        for(ResourceEnvironmentReferenceMetaData envEntry : entries) {
            final String name;
            if(envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getName();
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
            description.setBindingType(classType.getName());

            if(!isEmpty(envEntry.getLookupName())) {
                if(componentDescription != null) {
                    description.setReferenceSourceDescription(new LookupBindingSourceDescription(envEntry.getLookupName(),componentDescription));
                } else {
                    description.setReferenceSourceDescription(new LookupBindingSourceDescription(envEntry.getLookupName(),moduleDescription));
                }
            } else {
                //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                description.setReferenceSourceDescription(new LazyBindingSourceDescription());
            }
            bindings.add(description);
        }
        return bindings;
    }

    private List<BindingDescription> getResourceRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        List<BindingDescription> bindings = new ArrayList<BindingDescription>();
        final ResourceReferencesMetaData entries = environment.getEnvironment().getResourceReferences();
        if(entries == null) {
            return bindings;
        }
        for(ResourceReferenceMetaData envEntry : entries) {
            final String name;
            if(envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getName();
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
            description.setBindingType(classType.getName());

            if(!isEmpty(envEntry.getLookupName())) {
                if(componentDescription != null) {
                    description.setReferenceSourceDescription(new LookupBindingSourceDescription(envEntry.getLookupName(),componentDescription));
                } else {
                    description.setReferenceSourceDescription(new LookupBindingSourceDescription(envEntry.getLookupName(),moduleDescription));
                }
            } else {
                //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                description.setReferenceSourceDescription(new LazyBindingSourceDescription());
            }
            bindings.add(description);
        }
        return bindings;
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
            if(classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for <env-entry> " + name + " please specify the <env-entry-type>. Component");
            }

            final String value = envEntry.getValue();

            if(isEmpty(value) ) {
                //if no value is provided then it is not an error
                //this reference should simply be ignored
                // (Java ee platform spec 6.0 fr pg 80)
                continue;
            }

            final String type = classType.getName();
            description.setBindingType(classType.getName());

            if(type.equals(String.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(value));
            } else if(type.equals(Integer.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Integer.valueOf(value)));
            } else if(type.equals(Short.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Short.valueOf(value)));
            } else if(type.equals(Long.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Long.valueOf(value)));
            } else if(type.equals(Byte.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Byte.valueOf(value)));
            }  else if(type.equals(Double.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Double.valueOf(value)));
            } else if(type.equals(Float.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Float.valueOf(value)));
            } else if(type.equals(Boolean.class.getName())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Boolean.valueOf(value)));
            } else if(type.equals(Character.class.getName())) {
                if(value.length() != 1) {
                    throw new DeploymentUnitProcessingException("env-entry of type java.lang.Character is not exactly one character long " + value);
                }
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(value.charAt(0)));
            } else if(type.equals(Class.class.getName())) {
                try {
                    description.setReferenceSourceDescription(new EnvEntryInjectionSource(classLoader.loadClass(value)));
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load class " + value + " specified in env-entry");
                }
            }  else if(classType.isEnum() || (classType.getEnclosingClass() != null && classType.getEnclosingClass().isEnum())) {
                description.setReferenceSourceDescription(new EnvEntryInjectionSource(Enum.valueOf((Class)classType,value)));
            } else {
                throw new DeploymentUnitProcessingException("Unkown env-entry type " + type);
            }
            bindings.add(description);
        }
        return bindings;
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }



    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
