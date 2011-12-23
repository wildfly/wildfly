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
package org.jboss.as.ee.component.deployers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ClassDescriptionTraversal;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.naming.ContextInjectionSource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ee.EeLogger.ROOT_LOGGER;
import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * Processor that sets up JNDI bindings that are owned by the module. It also handles class level jndi bindings
 * that belong to components that do not have their own java:comp namespace, and class level bindings declared in
 * namespaces above java:comp.
 * <p/>
 * This processor is also responsible for throwing an exception if any ee component classes have been marked as invalid.
 *
 * @author Stuart Douglas
 */
public class
        ModuleJndiBindingProcessor implements DeploymentUnitProcessor {

    private static class IntHolder {
        private int value = 0;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        if (moduleConfiguration == null) {
            return;
        }
        final Set<ServiceName> dependencies = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES);

        final Map<ServiceName, BindingConfiguration> deploymentDescriptorBindings = new HashMap<ServiceName, BindingConfiguration>();

        // bindings
        // Handle duplicates binding from the same source
        // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
        // handle these duplicates?
        IntHolder moduleCount = new IntHolder();
        final List<BindingConfiguration> bindingConfigurations = eeModuleDescription.getBindingConfigurations();

        //we need to make sure that java:module/env and java:comp/env are always available
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            bindingConfigurations.add(new BindingConfiguration("java:module/env", new ContextInjectionSource("env", "java:module/env")));
        }
        if (deploymentUnit.getParent() == null) {
            bindingConfigurations.add(new BindingConfiguration("java:app/env", new ContextInjectionSource("env", "java:app/env")));
        }


        final ServiceName moduleOwnerName = deploymentUnit.getServiceName().append("module").append(moduleConfiguration.getApplicationName()).append(moduleConfiguration.getModuleName());
        for (BindingConfiguration binding : bindingConfigurations) {

            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());

            deploymentDescriptorBindings.put(bindInfo.getBinderServiceName(), binding);
            addJndiBinding(moduleConfiguration, binding, phaseContext, bindInfo.getBinderServiceName(), moduleOwnerName, moduleCount, dependencies);
        }

        //now we process all component level bindings, for components that do not have their own java:comp namespace.
        // these are bindings that have been added via a deployment descriptor
        for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {
            // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
            // handle these duplicates?
            for (BindingConfiguration binding : componentConfiguration.getComponentDescription().getBindingConfigurations()) {
                final String bindingName = binding.getName();
                final boolean compBinding = bindingName.startsWith("java:comp") || !bindingName.startsWith("java:");
                if (componentConfiguration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE && compBinding) {
                    //components with there own comp context do their own binding
                    continue;
                }

                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());
                deploymentDescriptorBindings.put(bindInfo.getBinderServiceName(), binding);
                addJndiBinding(moduleConfiguration, binding, phaseContext, bindInfo.getBinderServiceName(), moduleOwnerName, moduleCount, dependencies);
            }
        }

        //now add all class level bindings
        final Set<String> handledClasses = new HashSet<String>();

        for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {
            final Set<Class<?>> classConfigurations = new HashSet<Class<?>>();
            classConfigurations.add(componentConfiguration.getComponentClass());

            for (final InterceptorDescription interceptor : componentConfiguration.getComponentDescription().getAllInterceptors()) {
                try {
                    final ClassIndex interceptorClass = classIndex.classIndex(interceptor.getInterceptorClassName());
                    classConfigurations.add(interceptorClass.getModuleClass());
                } catch (ClassNotFoundException e) {
                    throw MESSAGES.cannotLoadInterceptor(e, interceptor.getInterceptorClassName(), componentConfiguration.getComponentClass());
                }
            }
            processClassConfigurations(phaseContext, applicationClasses, moduleConfiguration, deploymentDescriptorBindings, handledClasses, componentConfiguration.getComponentDescription().getNamingMode(), classConfigurations, componentConfiguration.getComponentName(), moduleOwnerName, moduleCount, dependencies);
        }

    }

    private void processClassConfigurations(final DeploymentPhaseContext phaseContext, final EEApplicationClasses applicationClasses, final EEModuleConfiguration moduleConfiguration, final Map<ServiceName, BindingConfiguration> deploymentDescriptorBindings, final Set<String> handledClasses, final ComponentNamingMode namingMode, final Set<Class<?>> classes, final String componentName, final ServiceName ownerName, final IntHolder handleCount, final Set<ServiceName> dependencies) throws DeploymentUnitProcessingException {
        for (final Class<?> clazz : classes) {
            new ClassDescriptionTraversal(clazz, applicationClasses) {
                @Override
                protected void handle(final Class<?> currentClass, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                    if (classDescription == null) {
                        return;
                    }
                    if (classDescription.isInvalid()) {
                        throw MESSAGES.componentClassHasErrors(classDescription.getClassName(), componentName, classDescription.getInvalidMessage());
                    }
                    //only process classes once
                    if (handledClasses.contains(classDescription.getClassName())) {
                        return;
                    }
                    handledClasses.add(classDescription.getClassName());
                    // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
                    // handle these duplicates?
                    if (!MetadataCompleteMarker.isMetadataComplete(phaseContext.getDeploymentUnit())) {
                        final Set<BindingConfiguration> classLevelBindings = new HashSet<BindingConfiguration>(classDescription.getBindingConfigurations());
                        for (BindingConfiguration binding : classLevelBindings) {
                            final String bindingName = binding.getName();
                            final boolean compBinding = bindingName.startsWith("java:comp") || !bindingName.startsWith("java:");
                            if (namingMode == ComponentNamingMode.CREATE && compBinding) {
                                //components with their own comp context do their own binding
                                continue;
                            }
                            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());

                            ROOT_LOGGER.tracef("Binding %s using service %s", binding.getName(), bindInfo.getBinderServiceName());

                            if (deploymentDescriptorBindings.containsKey(bindInfo.getBinderServiceName())) {
                                continue; //this has been overridden by a DD binding
                            }
                            addJndiBinding(moduleConfiguration, binding, phaseContext, bindInfo.getBinderServiceName(), ownerName, handleCount, dependencies);
                        }
                    }
                }
            }.run();
        }
    }


    protected void addJndiBinding(final EEModuleConfiguration module, final BindingConfiguration bindingConfiguration, final DeploymentPhaseContext phaseContext, ServiceName serviceName, ServiceName ownerName, IntHolder handleCount, final Set<ServiceName> dependencies) throws DeploymentUnitProcessingException {
        // Gather information about the dependency
        final String bindingName = bindingConfiguration.getName().startsWith("java:") ? bindingConfiguration.getName() : "java:module/env/" + bindingConfiguration.getName();

        final ServiceVerificationHandler serviceVerificationHandler = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.SERVICE_VERIFICATION_HANDLER);

        InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                true,
                module.getModuleName(),
                module.getModuleName(),
                module.getApplicationName()
        );

        // Check to see if this entry should actually be bound into JNDI.
        if (bindingName != null) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(module.getApplicationName(), module.getModuleName(), module.getModuleName(), false, bindingName);

            if (bindingName.startsWith("java:comp") || bindingName.startsWith("java:module") || bindingName.startsWith("java:app")) {
                //this is a binding that does not need to be shared.

                try {
                    final BinderService service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource());
                    dependencies.add(bindInfo.getBinderServiceName());
                    ServiceBuilder<ManagedReferenceFactory> serviceBuilder = phaseContext.getServiceTarget().addService(bindInfo.getBinderServiceName(), service);
                    bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                    serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                    serviceBuilder.install();
                } catch (DuplicateServiceException e) {
                    ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
                    if (registered == null)
                        throw e;

                    BinderService service = (BinderService) registered.getService();
                    if (!service.getSource().equals(bindingConfiguration.getSource()))
                        throw MESSAGES.conflictingBinding(bindingName, bindingConfiguration.getSource());
                } catch (CircularDependencyException e) {
                    throw MESSAGES.circularDependency(bindingName);
                }

            } else {
                ServiceController<ManagedReferenceFactory> controller = null;
                BinderService service;
                try {
                    service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource());
                    dependencies.add(bindInfo.getBinderServiceName());
                    ServiceBuilder<ManagedReferenceFactory> serviceBuilder = CurrentServiceContainer.getServiceContainer().addService(bindInfo.getBinderServiceName(), service);
                    bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                    serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                    serviceBuilder.addListener(serviceVerificationHandler);
                    controller = serviceBuilder.install();

                    service.acquire();
                } catch (DuplicateServiceException e) {
                    controller = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
                    if (controller == null)
                        throw e;

                    service = (BinderService) controller.getService();
                    if (!service.getSource().equals(bindingConfiguration.getSource())) {
                        throw MESSAGES.conflictingBinding(bindingName, bindingConfiguration.getSource());
                    }
                    service.acquire();
                }
                //as these bindings are not child services
                //we need to add a listener that released the service when the deployment stops
                ServiceController<?> unitService = CurrentServiceContainer.getServiceContainer().getService(phaseContext.getDeploymentUnit().getServiceName());
                final BinderService binderService = service;
                unitService.addListener(new BinderReleaseListener(binderService));
            }

        } else {
            throw MESSAGES.nullBindingName(bindingConfiguration);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private static class BinderReleaseListener<T> extends AbstractServiceListener<T> {

        private final BinderService binderService;

        public BinderReleaseListener(final BinderService binderService) {
            this.binderService = binderService;
        }

        @Override
        public void listenerAdded(final ServiceController<? extends T> serviceController) {
            if (serviceController.getState() == ServiceController.State.DOWN || serviceController.getState() == ServiceController.State.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }

        @Override
        public void transition(final ServiceController<? extends T> serviceController, final ServiceController.Transition transition) {
            if (transition.getAfter() == ServiceController.Substate.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }
    }
}
