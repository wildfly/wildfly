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
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.metadata.javaee.support.ResourceInjectionMetaDataWithDescriptions;
import org.jboss.modules.Module;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Class that provides common functionality required by processors that process environment information from deployment descriptors.
 *
 * @author Stuart Douglas
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
            handleLazyBindings(description, bindings);
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
                handleLazyBindings(description, bindings);
                componentDescription.getBindingConfigurations().addAll(bindings);
            }
        }

    }

    private void handleLazyBindings(final EEModuleDescription description, final List<BindingConfiguration> bindings) {
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
    protected Class<?> processInjectionTargets(EEModuleDescription moduleDescription, final EEApplicationClasses applicationClasses, InjectionSource injectionSource, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionMetaDataWithDescriptions entry, Class<?> classType) throws DeploymentUnitProcessingException {

        if (entry.getInjectionTargets() != null) {
            for (ResourceInjectionTargetMetaData injectionTarget : entry.getInjectionTargets()) {

                final Class<?> injectionTargetClass;
                try {
                    injectionTargetClass = classLoader.loadClass(injectionTarget.getInjectionTargetClass());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load " + injectionTarget.getInjectionTargetClass() + " referenced in env-entry injection point ", e);
                }
                EEModuleClassDescription eeModuleClassDescription = applicationClasses.getOrAddClassByName(injectionTargetClass.getName());
                final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(injectionTargetClass);
                String methodName = "set" + injectionTarget.getInjectionTargetName().substring(0, 1).toUpperCase() + injectionTarget.getInjectionTargetName().substring(1);

                boolean methodFound = false;
                Method method = null;
                Field field = null;
                Class<?> injectionTargetType = null;
                String memberName = injectionTarget.getInjectionTargetName();
                Class<?> current = injectionTargetClass;
                while (current != Object.class && current != null && !methodFound) {
                    final Collection<Method> methods = index.getAllMethods(methodName);
                    for (Method m : methods) {
                        if (m.getParameterTypes().length == 1) {
                            if (m.isBridge() || m.isSynthetic()) {
                                continue;
                            }
                            if (methodFound) {
                                throw new DeploymentUnitProcessingException("Two setter methods for " + injectionTarget.getInjectionTargetName() + " on class " + injectionTarget.getInjectionTargetClass() + " found when applying <injection-target> for env-entry");
                            }
                            methodFound = true;
                            method = m;
                            injectionTargetType = m.getParameterTypes()[0];
                            memberName = methodName;
                        }
                    }
                    current = current.getSuperclass();
                }
                if (method == null) {
                    current = injectionTargetClass;
                    while (current != Object.class && current != null && field == null) {
                        field = index.getField(injectionTarget.getInjectionTargetName());
                        if (field != null) {
                            injectionTargetType = field.getType();
                            memberName = injectionTarget.getInjectionTargetName();
                            break;
                        }
                        current = current.getSuperclass();
                    }
                }
                if (field == null && method == null) {
                    throw new DeploymentUnitProcessingException("Could not resolve injection point " + injectionTarget.getInjectionTargetName() + " on class " + injectionTarget.getInjectionTargetClass() + " specified in web.xml");
                }
                if (classType != null) {
                    if (!classType.isAssignableFrom(injectionTargetType)) {
                        throw new DeploymentUnitProcessingException("Injection target " + injectionTarget.getInjectionTargetName() + " on class " + injectionTarget.getInjectionTargetClass() + " is not compatible with the type of injection");
                    }
                } else {
                    classType = injectionTargetType;
                }
                final InjectionTarget injectionTargetDescription = method == null ?
                        new FieldInjectionTarget(eeModuleClassDescription.getClassName(), memberName, classType.getName()) :
                        new MethodInjectionTarget(eeModuleClassDescription.getClassName(), memberName, classType.getName());

                final ResourceInjectionConfiguration injectionConfiguration = new ResourceInjectionConfiguration(injectionTargetDescription, injectionSource);
                eeModuleClassDescription.getConfigurators().add(new ClassConfigurator() {
                    @Override
                    public void configure(DeploymentPhaseContext context, EEModuleClassDescription description, EEModuleClassConfiguration configuration) throws DeploymentUnitProcessingException {
                        configuration.getInjectionConfigurations().add(injectionConfiguration);
                    }
                });

            }
        }
        return classType;
    }
}
