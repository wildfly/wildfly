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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.InjectionConfigurator;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LazyResourceInjection;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.ResourceInjectionMetaData;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.modules.Module;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Class that provides common functionality required by processors that process environment information from deployment descriptors.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractDeploymentDescriptorBindingsProcessor implements DeploymentUnitProcessor {

    @Override
    public final void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEModuleDescription description = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (description == null) {
            return;
        }


        if (environment != null) {
            final List<BindingConfiguration> bindings = processDescriptorEntries(deploymentUnit, environment, description, null, module.getClassLoader(), deploymentReflectionIndex, applicationClasses);
            handleLazyBindings(applicationClasses, bindings);
            description.getConfigurators().add(new EEModuleConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, EEModuleDescription description, EEModuleConfiguration configuration) throws DeploymentUnitProcessingException {
                    configuration.getBindingConfigurations().addAll(bindings);
                }
            });
        }
        for (final ComponentDescription componentDescription : description.getComponentDescriptions()) {
            if (componentDescription.getDeploymentDescriptorEnvironment() != null) {
                final List<BindingConfiguration> bindings = processDescriptorEntries(deploymentUnit, componentDescription.getDeploymentDescriptorEnvironment(), description, componentDescription, module.getClassLoader(), deploymentReflectionIndex, applicationClasses);
                handleLazyBindings(applicationClasses, bindings);
                componentDescription.getBindingConfigurations().addAll(bindings);
            }
        }

    }

    private void handleLazyBindings(final EEApplicationClasses description, final List<BindingConfiguration> bindings) {
        for (final BindingConfiguration binding : bindings) {
            String name = binding.getName();
            if (!name.startsWith("java:")) {
                name = "java:comp/" + name;
            }
            final List<LazyResourceInjection> lazyInjections = description.getLazyResourceInjections().get(name);
            if (lazyInjections != null) {
                for (final LazyResourceInjection injection : lazyInjections) {
                    injection.install();
                }
                description.getLazyResourceInjections().remove(name);
            }
        }
    }

    protected abstract List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, EEModuleDescription moduleDescription, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException;

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Processes the injection targets of a resource binding
     *
     *
     * @param applicationClasses
     * @param injectionSource           The injection source for the injection target
     * @param classLoader               The module class loader
     * @param deploymentReflectionIndex The deployment reflection index
     * @param entry                     The resource with injection targets
     * @param classType                 The expected type of the injection point, may be null if this is to be inferred from the injection target
     * @return The actual class type of the injection point
     * @throws DeploymentUnitProcessingException
     *          If the injection points could not be resolved
     */
    protected Class<?> processInjectionTargets(EEModuleDescription moduleDescription, final EEApplicationClasses applicationClasses, InjectionSource injectionSource, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionMetaData entry, Class<?> classType) throws DeploymentUnitProcessingException {
        if (entry.getInjectionTargets() != null) {
            for (ResourceInjectionTargetMetaData injectionTarget : entry.getInjectionTargets()) {
                final String injectionTargetClassName = injectionTarget.getInjectionTargetClass();
                final String injectionTargetName = injectionTarget.getInjectionTargetName();
                final AccessibleObject fieldOrMethod = getInjectionTarget(injectionTargetClassName, injectionTargetName, classLoader, deploymentReflectionIndex);
                final Class<?> injectionTargetType = fieldOrMethod instanceof Field ? ((Field)fieldOrMethod).getType() : ((Method)fieldOrMethod).getParameterTypes()[0];
                final String memberName = fieldOrMethod instanceof Field ? ((Field)fieldOrMethod).getName() : ((Method)fieldOrMethod).getName();

                if (classType != null) {
                    if (!classType.isAssignableFrom(injectionTargetType)) {
                        throw new DeploymentUnitProcessingException("Injection target " + injectionTarget.getInjectionTargetName() + " on class " + injectionTarget.getInjectionTargetClass() + " is not compatible with the type of injection");
                    }
                } else {
                    classType = injectionTargetType;
                }
                final InjectionTarget injectionTargetDescription = fieldOrMethod instanceof Field ?
                        new FieldInjectionTarget(injectionTargetClassName, memberName, classType.getName()) :
                        new MethodInjectionTarget(injectionTargetClassName, memberName, classType.getName());

                final ResourceInjectionConfiguration injectionConfiguration = new ResourceInjectionConfiguration(injectionTargetDescription, injectionSource);
                EEModuleClassDescription eeModuleClassDescription = applicationClasses.getOrAddClassByName(injectionTargetClassName);
                eeModuleClassDescription.getConfigurators().add(new InjectionConfigurator(injectionConfiguration));
            }
        }
        return classType;
    }

    protected static AccessibleObject getInjectionTarget(final String injectionTargetClassName, final String injectionTargetName, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final Class<?> injectionTargetClass;
        try {
            injectionTargetClass = classLoader.loadClass(injectionTargetClassName);
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Could not load " + injectionTargetClassName + " referenced in env-entry injection point ", e);
        }
        final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(injectionTargetClass);
        String methodName = "set" + injectionTargetName.substring(0, 1).toUpperCase() + injectionTargetName.substring(1);

        boolean methodFound = false;
        Method method = null;
        Field field = null;
        Class<?> current = injectionTargetClass;
        while (current != Object.class && current != null && !methodFound) {
            final Collection<Method> methods = index.getAllMethods(methodName);
            for (Method m : methods) {
                if (m.getParameterTypes().length == 1) {
                    if (m.isBridge() || m.isSynthetic()) {
                        continue;
                    }
                    if (methodFound) {
                        throw new DeploymentUnitProcessingException("Two setter methods for " + injectionTargetName + " on class " + injectionTargetClassName + " found when applying <injection-target> for env-entry");
                    }
                    methodFound = true;
                    method = m;
                }
            }
            current = current.getSuperclass();
        }
        if (method == null) {
            current = injectionTargetClass;
            while (current != Object.class && current != null && field == null) {
                field = index.getField(injectionTargetName);
                if (field != null) {
                    break;
                }
                current = current.getSuperclass();
            }
        }
        if (field == null && method == null) {
            throw new DeploymentUnitProcessingException("Could not resolve injection point " + injectionTargetName + " on class " + injectionTargetClassName + " specified in web.xml");
        }

        return field != null ? field : method;
    }

}
