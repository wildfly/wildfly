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
 * Deployment processor that sets up env-entry, resource-ref and resource-env-ref bindings
 *
 * @author Stuart Douglas
 */
public class ResourceReferenceProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, EEModuleDescription moduleDescription, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        bindings.addAll(getEnvironmentEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription));
        bindings.addAll(getResourceEnvRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription));
        bindings.addAll(getResourceRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription));
        return bindings;
    }

    private List<BindingConfiguration> getResourceEnvRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final ResourceEnvironmentReferencesMetaData entries = environment.getEnvironment().getResourceEnvironmentReferences();
        if (entries == null) {
            return bindings;
        }
        for (ResourceEnvironmentReferenceMetaData envEntry : entries) {
            final String name;
            if (envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getName();
            }
            Class<?> classType = null;
            if (envEntry.getType() != null) {
                try {
                    classType = classLoader.loadClass(envEntry.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + envEntry.getType() + " referenced in env-entry ", e);
                }
            }
            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);

            classType = processInjectionTargets(moduleDescription, injectionSource, classLoader, deploymentReflectionIndex, envEntry, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for resource-env-ref " + name);
            }
            BindingConfiguration bindingConfiguration = null;
            if (!isEmpty(envEntry.getLookupName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(envEntry.getLookupName()));
            } else {
                //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                throw new RuntimeException("res-env-ref without a lookup-name isn't yet supported");
            }
            bindings.add(bindingConfiguration);
        }
        return bindings;
    }

    private List<BindingConfiguration> getResourceRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final ResourceReferencesMetaData entries = environment.getEnvironment().getResourceReferences();
        if (entries == null) {
            return bindings;
        }
        for (ResourceReferenceMetaData envEntry : entries) {
            final String name;
            if (envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getName();
            }

            Class<?> classType = null;
            if (envEntry.getType() != null) {
                try {
                    classType = classLoader.loadClass(envEntry.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + envEntry.getType() + " referenced in env-entry ", e);
                }
            }

            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(moduleDescription, injectionSource, classLoader, deploymentReflectionIndex, envEntry, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for resource-ref " + name);
            }
            BindingConfiguration bindingConfiguration = null;
            if (!isEmpty(envEntry.getLookupName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(envEntry.getLookupName()));
            } else {
                //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                throw new RuntimeException("resource-ref without a lookup-name isn't yet supported");
            }
            bindings.add(bindingConfiguration);
        }
        return bindings;
    }

    private List<BindingConfiguration> getEnvironmentEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final EnvironmentEntriesMetaData entries = environment.getEnvironment().getEnvironmentEntries();
        if (entries == null) {
            return bindings;
        }
        for (EnvironmentEntryMetaData envEntry : entries) {
            final String name;
            if (envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getEnvEntryName();
            }

            Class<?> classType = null;
            if (envEntry.getType() != null) {
                try {
                    classType = classLoader.loadClass(envEntry.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + envEntry.getType() + " referenced in env-entry ", e);
                }
            }

            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(moduleDescription, injectionSource, classLoader, deploymentReflectionIndex, envEntry, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for <env-entry> " + name + " please specify the <env-entry-type>.");
            }

            final String value = envEntry.getValue();

            if (isEmpty(value)) {
                //if no value is provided then it is not an error
                //this reference should simply be ignored
                // (Java ee platform spec 6.0 fr pg 80)
                continue;
            }

            final String type = classType.getName();
            BindingConfiguration bindingConfiguration = null;
            if (type.equals(String.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(value));
            } else if (type.equals(Integer.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Integer.valueOf(value)));
            } else if (type.equals(Short.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Short.valueOf(value)));
            } else if (type.equals(Long.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Long.valueOf(value)));
            } else if (type.equals(Byte.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Byte.valueOf(value)));
            } else if (type.equals(Double.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Double.valueOf(value)));
            } else if (type.equals(Float.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Float.valueOf(value)));
            } else if (type.equals(Boolean.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Boolean.valueOf(value)));
            } else if (type.equals(Character.class.getName())) {
                if (value.length() != 1) {
                    throw new DeploymentUnitProcessingException("env-entry of type java.lang.Character is not exactly one character long " + value);
                }
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(value.charAt(0)));
            } else if (type.equals(Class.class.getName())) {
                try {
                    bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(classLoader.loadClass(value)));
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load class " + value + " specified in env-entry");
                }
            } else if (classType.isEnum() || (classType.getEnclosingClass() != null && classType.getEnclosingClass().isEnum())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Enum.valueOf((Class) classType, value)));
            } else {
                throw new DeploymentUnitProcessingException("Unkown env-entry type " + type);
            }
            bindings.add(bindingConfiguration);
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
