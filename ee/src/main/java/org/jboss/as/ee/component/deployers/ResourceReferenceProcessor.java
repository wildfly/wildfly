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
package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EnvEntryInjectionSource;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationReferenceMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationReferencesMetaData;
import org.jboss.metadata.javaee.spec.ResourceEnvironmentReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceEnvironmentReferencesMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Deployment processor that sets up env-entry, resource-ref and resource-env-ref bindings
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ResourceReferenceProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    private static final String JAVAX_NAMING_CONTEXT = "javax.naming.Context";

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ResourceInjectionTarget resourceInjectionTarget, final ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        bindings.addAll(getEnvironmentEntries(environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget));
        bindings.addAll(getResourceEnvRefEntries(deploymentUnit, environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget));
        bindings.addAll(getResourceRefEntries(deploymentUnit, environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget));
        bindings.addAll(getMessageDestinationRefs(environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget, deploymentUnit));
        return bindings;
    }

    private List<BindingConfiguration> getResourceEnvRefEntries(final DeploymentUnit deploymentUnit, final DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionTarget resourceInjectionTarget) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final ResourceEnvironmentReferencesMetaData resourceEnvRefs = environment.getEnvironment().getResourceEnvironmentReferences();
        final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
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
                    throw EeLogger.ROOT_LOGGER.cannotLoad(e, resourceEnvRef.getType());
                }
            }
            // our injection (source) comes from the local (ENC) lookup, no matter what.
            InjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, resourceEnvRef, classType);
            if (!isEmpty(resourceEnvRef.getLookupName())) {
                injectionSource = new LookupInjectionSource(resourceEnvRef.getLookupName(), classType != null && JAVAX_NAMING_CONTEXT.equals(classType.getName()));
            } else {
                if (classType == null) {
                    throw EeLogger.ROOT_LOGGER.cannotDetermineType(name);
                }
                //check if it is a well known type
                final String lookup = ResourceInjectionAnnotationParsingProcessor.FIXED_LOCATIONS.get(classType.getName());
                if (lookup != null) {
                    injectionSource = new LookupInjectionSource(lookup);
                } else {
                    final EEResourceReferenceProcessor resourceReferenceProcessor = registry.getResourceReferenceProcessor(classType.getName());
                    if (resourceReferenceProcessor != null) {
                        injectionSource = resourceReferenceProcessor.getResourceReferenceBindingSource();
                    } else {
                        //TODO: how are we going to handle these? Previously they would have been handled by jboss-*.xml
                        if (resourceEnvRef.getResourceEnvRefName().startsWith("java:")) {
                            ROOT_LOGGER.cannotResolve("resource-env-ref", name);
                            continue;
                        } else {
                            injectionSource = new LookupInjectionSource("java:jboss/resources/" + resourceEnvRef.getResourceEnvRefName());
                        }
                    }
                }
            }
            bindings.add(new BindingConfiguration(name, injectionSource));
        }
        return bindings;
    }

    private List<BindingConfiguration> getResourceRefEntries(final DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionTarget resourceInjectionTarget) throws DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
        final ResourceReferencesMetaData resourceRefs = environment.getEnvironment().getResourceReferences();
        if (resourceRefs == null) {
            return bindings;
        }
        for (final ResourceReferenceMetaData resourceRef : resourceRefs) {
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
                    throw EeLogger.ROOT_LOGGER.cannotLoad(e, resourceRef.getType());
                }
            }
            // our injection (source) comes from the local (ENC) lookup, no matter what.
            InjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, resourceRef, classType);
            if (!isEmpty(resourceRef.getLookupName())) {
                injectionSource = new LookupInjectionSource(resourceRef.getLookupName(), classType != null && JAVAX_NAMING_CONTEXT.equals(classType.getName()));
            } else if (!isEmpty(resourceRef.getResUrl())) {
                final String url = resourceRef.getResUrl();
                if (classType != null && classType.equals(URI.class)) {
                    try {
                        //we need a newURI every time
                        injectionSource = new FixedInjectionSource(new ManagedReferenceFactory() {
                            @Override
                            public ManagedReference getReference() {
                                try {
                                    return new ImmediateManagedReference(new URI(url));
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }, new URI(url));
                    } catch (URISyntaxException e) {
                        throw EeLogger.ROOT_LOGGER.cannotParseResourceRefUri(e, resourceRef.getResUrl());
                    }
                } else {
                    try {
                        injectionSource = new FixedInjectionSource(new ManagedReferenceFactory() {
                            @Override
                            public ManagedReference getReference() {
                                try {
                                    return new ImmediateManagedReference(new URL(url));
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }, new URL(url));
                    } catch (MalformedURLException e) {
                        throw EeLogger.ROOT_LOGGER.cannotParseResourceRefUri(e, resourceRef.getResUrl());
                    }
                }
            } else {
                if (classType == null) {
                    throw EeLogger.ROOT_LOGGER.cannotDetermineType(name);
                }
                //check if it is a well known type
                final String lookup = ResourceInjectionAnnotationParsingProcessor.FIXED_LOCATIONS.get(classType.getName());
                if (lookup != null) {
                    injectionSource = new LookupInjectionSource(lookup);
                } else {
                    final EEResourceReferenceProcessor resourceReferenceProcessor = registry.getResourceReferenceProcessor(classType.getName());
                    if (resourceReferenceProcessor != null) {
                        injectionSource = resourceReferenceProcessor.getResourceReferenceBindingSource();
                    } else if (!resourceRef.getResourceRefName().startsWith("java:")) {
                        injectionSource = new LookupInjectionSource("java:jboss/resources/" + resourceRef.getResourceRefName());
                    } else {
                        //if we cannot resolve it just log
                        ROOT_LOGGER.cannotResolve("resource-env-ref", name);
                        continue;
                    }
                }
            }
            bindings.add(new BindingConfiguration(name, injectionSource));
        }
        return bindings;
    }

    private List<BindingConfiguration> getEnvironmentEntries(final DeploymentDescriptorEnvironment environment, final ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionTarget resourceInjectionTarget) throws DeploymentUnitProcessingException {
        final List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final EnvironmentEntriesMetaData envEntries = environment.getEnvironment().getEnvironmentEntries();
        if (envEntries == null) {
            return bindings;
        }
        for (final EnvironmentEntryMetaData envEntry : envEntries) {
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
                    throw EeLogger.ROOT_LOGGER.cannotLoad(e, envEntry.getType());
                }
            }
            final String value = envEntry.getValue();
            final String lookup = envEntry.getLookupName();
            if (!isEmpty(value) && !isEmpty(lookup)) {
                throw EeLogger.ROOT_LOGGER.cannotSpecifyBoth("<env-entry-value>", "<lookup-name>");
            } else if (isEmpty(lookup) && isEmpty(value)) {
                //if no value is provided then it is not an error
                //this reference should simply be ignored
                // (Java ee platform spec 6.0 fr pg 80)
                continue;
            }

            // our injection (source) comes from the local (ENC) lookup, no matter what.
            LookupInjectionSource injectionSource = new LookupInjectionSource(name);
            classType = processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, envEntry, classType);
            if (classType == null) {
                throw EeLogger.ROOT_LOGGER.cannotDetermineType("<env-entry>", name, "<env-entry-type>");
            }


            final String type = classType.getName();
            final BindingConfiguration bindingConfiguration;
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
                    throw EeLogger.ROOT_LOGGER.invalidCharacterLength("env-entry", value);
                }
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(value.charAt(0)));
            } else if (type.equals(Class.class.getName())) {
                try {
                    bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(classLoader.loadClass(value)));
                } catch (ClassNotFoundException e) {
                    throw EeLogger.ROOT_LOGGER.cannotLoad(value);
                }
            } else if (classType.isEnum() || (classType.getEnclosingClass() != null && classType.getEnclosingClass().isEnum())) {
                bindingConfiguration = new BindingConfiguration(name, new EnvEntryInjectionSource(Enum.valueOf((Class) classType, value)));
            } else {
                throw EeLogger.ROOT_LOGGER.unknownElementType("env-entry", type);
            }
            bindings.add(bindingConfiguration);
        }
        return bindings;
    }

    /**
     * TODO: should this be part of the messaging subsystem
     */
    private List<BindingConfiguration> getMessageDestinationRefs(final DeploymentDescriptorEnvironment environment, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final ResourceInjectionTarget resourceInjectionTarget, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        final MessageDestinationReferencesMetaData messageDestinationReferences = environment.getEnvironment().getMessageDestinationReferences();
        if (messageDestinationReferences == null) {
            return bindings;
        }
        for (final MessageDestinationReferenceMetaData messageRef : messageDestinationReferences) {
            final String name;
            if (messageRef.getName().startsWith("java:")) {
                name = messageRef.getName();
            } else {
                name = environment.getDefaultContext() + messageRef.getName();
            }
            Class<?> classType = null;
            if (messageRef.getType() != null) {
                try {
                    classType = classLoader.loadClass(messageRef.getType());
                } catch (ClassNotFoundException e) {
                    throw EeLogger.ROOT_LOGGER.cannotLoad(e, messageRef.getType());
                }
            }
            // our injection (source) comes from the local (ENC) lookup, no matter what.
            final LookupInjectionSource injectionSource = new LookupInjectionSource(name);

            classType = processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, messageRef, classType);
            final BindingConfiguration bindingConfiguration;
            if (!isEmpty(messageRef.getLookupName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(messageRef.getLookupName()));
                bindings.add(bindingConfiguration);
            } else if (!isEmpty(messageRef.getMappedName())) {
                bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(messageRef.getMappedName()));
                bindings.add(bindingConfiguration);
            } else if (!isEmpty(messageRef.getLink())) {
                final MessageDestinationInjectionSource messageDestinationInjectionSource = new MessageDestinationInjectionSource(messageRef.getLink(), name);
                bindingConfiguration = new BindingConfiguration(name, messageDestinationInjectionSource);
                deploymentUnit.addToAttachmentList(Attachments.MESSAGE_DESTINATIONS, messageDestinationInjectionSource);
                bindings.add(bindingConfiguration);
            } else {
                ROOT_LOGGER.cannotResolve("message-destination-ref", name);
            }
        }
        return bindings;
    }

    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }


    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private Class<?> loadClass(String className, ClassLoader cl) throws ClassNotFoundException {
        if (className == null || className.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.cannotBeNullOrEmpty("Classname", className);
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
