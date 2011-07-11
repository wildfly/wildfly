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
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, EEModuleDescription moduleDescription, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        bindings.addAll(getEnvironmentEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription, applicationClasses));
        bindings.addAll(getResourceEnvRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription, applicationClasses));
        bindings.addAll(getResourceRefEntries(environment, classLoader, deploymentReflectionIndex, moduleDescription, componentDescription, applicationClasses));
        return bindings;
    }

    private List<BindingConfiguration> getResourceEnvRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final ResourceEnvironmentReferencesMetaData resourceEnvRefs = environment.getEnvironment().getResourceEnvironmentReferences();
        if (resourceEnvRefs == null) {
            return bindings;
        }
        for (ResourceEnvironmentReferenceMetaData resourceEnvRef : resourceEnvRefs) {
            final String name;
            if (resourceEnvRef.getName().startsWith("java:")) {
                name = resourceEnvRef.getName();
            } else {
                name = environment.getDefaultContext() + resourceEnvRef.getName();
            }
            Class<?> classType = null;
            if (resourceEnvRef.getType() != null) {
                try {
                    classType = classLoader.loadClass(resourceEnvRef.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + resourceEnvRef.getType() + " referenced in env-entry ", e);
                }
            }
            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);

            classType = processInjectionTargets(moduleDescription, applicationClasses, injectionSource, classLoader, deploymentReflectionIndex, resourceEnvRef, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for resource-env-ref " + name);
            }
            BindingConfiguration bindingConfiguration = null;
            if (!isEmpty(resourceEnvRef.getLookupName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(resourceEnvRef.getLookupName()));
            } else {
                //check if it is a well known type
                final String lookup = ResourceInjectionAnnotationParsingProcessor.FIXED_LOCATIONS.get(classType.getName());
                if (lookup != null) {
                    bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
                } else {
                    //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                    if (resourceEnvRef.getResourceEnvRefName().startsWith("java:")) {
                        bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(resourceEnvRef.getResourceEnvRefName()));
                    } else {
                        bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource("java:/" + resourceEnvRef.getResourceEnvRefName()));
                    }
                }
            }
            bindings.add(bindingConfiguration);
        }
        return bindings;
    }

    private List<BindingConfiguration> getResourceRefEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final ResourceReferencesMetaData resourceRefs = environment.getEnvironment().getResourceReferences();
        if (resourceRefs == null) {
            return bindings;
        }
        for (ResourceReferenceMetaData resourceRef : resourceRefs) {
            final String name;
            if (resourceRef.getName().startsWith("java:")) {
                name = resourceRef.getName();
            } else {
                name = environment.getDefaultContext() + resourceRef.getName();
            }

            Class<?> classType = null;
            if (resourceRef.getType() != null) {
                try {
                    classType = classLoader.loadClass(resourceRef.getType());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + resourceRef.getType() + " referenced in env-entry ", e);
                }
            }

            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(moduleDescription, applicationClasses, injectionSource, classLoader, deploymentReflectionIndex, resourceRef, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for resource-ref " + name);
            }
            BindingConfiguration bindingConfiguration = null;
            if (!isEmpty(resourceRef.getLookupName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(resourceRef.getLookupName()));
            } else {
                if (resourceRef.getResourceRefName().startsWith("java:")) {
                    bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource("java:jboss/resources/" + resourceRef.getResourceRefName()));
                } else {
                    bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(resourceRef.getResourceRefName()));
                }
            }
            bindings.add(bindingConfiguration);
        }
        return bindings;
    }

    private List<BindingConfiguration> getEnvironmentEntries(DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEModuleDescription moduleDescription, ComponentDescription componentDescription, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final EnvironmentEntriesMetaData envEntries = environment.getEnvironment().getEnvironmentEntries();
        if (envEntries == null) {
            return bindings;
        }
        for (EnvironmentEntryMetaData envEntry : envEntries) {
            final String name;
            if (envEntry.getName().startsWith("java:")) {
                name = envEntry.getName();
            } else {
                name = environment.getDefaultContext() + envEntry.getEnvEntryName();
            }

            Class<?> classType = null;
            if (envEntry.getType() != null) {
                try {
                    classType = this.loadClass(envEntry.getType(), classLoader);
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + envEntry.getType() + " referenced in env-entry ", e);
                }
            }
            final String value = envEntry.getValue();
            final String lookup = envEntry.getLookupName();
            if (!isEmpty(value) && !isEmpty(lookup)) {
                throw new DeploymentUnitProcessingException("Cannot specify both a <env-entry-value> and a <lookup-name> in an environemnt entry.");
            } else if (isEmpty(lookup) && isEmpty(value)) {
                //if no value is provided then it is not an error
                //this reference should simply be ignored
                // (Java ee platform spec 6.0 fr pg 80)
                continue;
            }

            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(moduleDescription, applicationClasses, injectionSource, classLoader, deploymentReflectionIndex, envEntry, classType);
            if (classType == null) {
                throw new DeploymentUnitProcessingException("Could not determine type for <env-entry> " + name + " please specify the <env-entry-type>.");
            }


            final String type = classType.getName();
            BindingConfiguration bindingConfiguration = null;
            if (!isEmpty(lookup)) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
            } else if (type.equals(String.class.getName())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(value));
            } else if (type.equals(Integer.class.getName()) || type.equals("int")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Integer.valueOf(value)));
            } else if (type.equals(Short.class.getName()) || type.equals("short")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Short.valueOf(value)));
            } else if (type.equals(Long.class.getName()) || type.equals("long")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Long.valueOf(value)));
            } else if (type.equals(Byte.class.getName()) || type.equals("byte")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Byte.valueOf(value)));
            } else if (type.equals(Double.class.getName()) || type.equals("double")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Double.valueOf(value)));
            } else if (type.equals(Float.class.getName()) || type.equals("float")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Float.valueOf(value)));
            } else if (type.equals(Boolean.class.getName()) || type.equals("boolean")) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Boolean.valueOf(value)));
            } else if (type.equals(Character.class.getName()) || type.equals("char")) {
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

    private Class<?> loadClass(String className, ClassLoader cl) throws ClassNotFoundException {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Classname cannot be null or empty: " + className);
        }
        if (className.equals(void.class.getName())) {
            return void.class;
        }
        if (className.equals(byte.class.getName())) {
            return byte.class;
        }
        if (className.equals(short.class.getName())) {
            return short.class;
        }
        if (className.equals(int.class.getName())) {
            return int.class;
        }
        if (className.equals(long.class.getName())) {
            return long.class;
        }
        if (className.equals(char.class.getName())) {
            return char.class;
        }
        if (className.equals(boolean.class.getName())) {
            return boolean.class;
        }
        if (className.equals(float.class.getName())) {
            return float.class;
        }
        if (className.equals(double.class.getName())) {
            return double.class;
        }
        // Now that we know its not a primitive, lets just allow
        // the passed classloader to handle the request.
        return Class.forName(className, false, cl);
    }
}
