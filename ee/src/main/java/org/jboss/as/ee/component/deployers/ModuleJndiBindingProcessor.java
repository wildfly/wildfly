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

import org.jboss.as.ee.logging.EeLogger;
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
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Processor that sets up JNDI bindings that are owned by the module. It also handles class level jndi bindings
 * that belong to components that do not have their own java:comp namespace, and class level bindings declared in
 * namespaces above java:comp.
 * <p/>
 * This processor is also responsible for throwing an exception if any ee component classes have been marked as invalid.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ModuleJndiBindingProcessor implements DeploymentUnitProcessor {

    private final boolean appclient;

    public ModuleJndiBindingProcessor(boolean appclient) {
        this.appclient = appclient;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (moduleConfiguration == null) {
            return;
        }
        final List<ServiceName> dependencies = deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES);

        final Map<ServiceName, BindingConfiguration> deploymentDescriptorBindings = new HashMap<ServiceName, BindingConfiguration>();

        final List<BindingConfiguration> bindingConfigurations = eeModuleDescription.getBindingConfigurations();

        //we need to make sure that java:module/env and java:comp/env are always available
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            bindingConfigurations.add(new BindingConfiguration("java:module/env", new ContextInjectionSource("env", "java:module/env")));
        }
        if (deploymentUnit.getParent() == null) {
            bindingConfigurations.add(new BindingConfiguration("java:app/env", new ContextInjectionSource("env", "java:app/env")));
        }

        for (BindingConfiguration binding : bindingConfigurations) {

            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());

            deploymentDescriptorBindings.put(bindInfo.getBinderServiceName(), binding);
            addJndiBinding(moduleConfiguration, binding, phaseContext, dependencies);
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
                addJndiBinding(moduleConfiguration, binding, phaseContext, dependencies);
            }
        }

        //now add all class level bindings
        final Set<String> handledClasses = new HashSet<String>();

        for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {
            final Set<Class<?>> classConfigurations = new HashSet<Class<?>>();
            classConfigurations.add(componentConfiguration.getComponentClass());

            for (final InterceptorDescription interceptor : componentConfiguration.getComponentDescription().getAllInterceptors()) {
                try {
                    classConfigurations.add(ClassLoadingUtils.loadClass(interceptor.getInterceptorClassName(), module));
                } catch (ClassNotFoundException e) {
                    throw EeLogger.ROOT_LOGGER.cannotLoadInterceptor(e, interceptor.getInterceptorClassName(), componentConfiguration.getComponentClass());
                }
            }
            processClassConfigurations(phaseContext, applicationClasses, moduleConfiguration, deploymentDescriptorBindings, handledClasses, componentConfiguration.getComponentDescription().getNamingMode(), classConfigurations, componentConfiguration.getComponentName(), dependencies);
        }
        //now we need to process resource bindings that are not part of a component
        //we don't process app client modules, as it just causes problems by installing bindings that
        //were only intended to be installed when running as an app client
        boolean appClient = DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit) || this.appclient;


        if (!MetadataCompleteMarker.isMetadataComplete(phaseContext.getDeploymentUnit()) && !appClient) {
            for (EEModuleClassDescription config : eeModuleDescription.getClassDescriptions()) {
                if (handledClasses.contains(config.getClassName())) {
                    continue;
                }
                if(config.isInvalid()) {
                    continue;
                }
                final Set<BindingConfiguration> classLevelBindings = new HashSet<>(config.getBindingConfigurations());
                for (BindingConfiguration binding : classLevelBindings) {
                    final String bindingName = binding.getName();
                    final boolean compBinding = bindingName.startsWith("java:comp") || !bindingName.startsWith("java:");
                    if(compBinding && !DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
                        //we don't have a comp namespace
                        continue;
                    }
                    final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());


                    if (deploymentDescriptorBindings.containsKey(bindInfo.getBinderServiceName())) {
                        continue; //this has been overridden by a DD binding
                    }
                    ROOT_LOGGER.tracef("Binding %s using service %s", binding.getName(), bindInfo.getBinderServiceName());
                    addJndiBinding(moduleConfiguration, binding, phaseContext, dependencies);
                }
            }
        }

    }

    private void processClassConfigurations(final DeploymentPhaseContext phaseContext, final EEApplicationClasses applicationClasses, final EEModuleConfiguration moduleConfiguration, final Map<ServiceName, BindingConfiguration> deploymentDescriptorBindings, final Set<String> handledClasses, final ComponentNamingMode namingMode, final Set<Class<?>> classes, final String componentName, final List<ServiceName> dependencies) throws DeploymentUnitProcessingException {
        for (final Class<?> clazz : classes) {
            new ClassDescriptionTraversal(clazz, applicationClasses) {
                @Override
                protected void handle(final Class<?> currentClass, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                    if (classDescription == null) {
                        return;
                    }
                    if (classDescription.isInvalid()) {
                        throw EeLogger.ROOT_LOGGER.componentClassHasErrors(classDescription.getClassName(), componentName, classDescription.getInvalidMessage());
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

                            if (deploymentDescriptorBindings.containsKey(bindInfo.getBinderServiceName())) {
                                continue; //this has been overridden by a DD binding
                            }
                            ROOT_LOGGER.tracef("Binding %s using service %s", binding.getName(), bindInfo.getBinderServiceName());
                            addJndiBinding(moduleConfiguration, binding, phaseContext, dependencies);
                        }
                    }
                }
            }.run();
        }
    }


    protected void addJndiBinding(final EEModuleConfiguration module, final BindingConfiguration bindingConfiguration, final DeploymentPhaseContext phaseContext, final List<ServiceName> dependencies) throws DeploymentUnitProcessingException {
        // Gather information about the dependency
        final String bindingName = bindingConfiguration.getName().startsWith("java:") ? bindingConfiguration.getName() : "java:module/env/" + bindingConfiguration.getName();

        InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                true,
                module.getModuleName(),
                module.getModuleName(),
                module.getApplicationName()
        );

        // Check to see if this entry should actually be bound into JNDI.
        if (bindingName != null) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(module.getApplicationName(), module.getModuleName(), module.getModuleName(), false, bindingName);
            dependencies.add(bindInfo.getBinderServiceName());
            if (bindingName.startsWith("java:comp") || bindingName.startsWith("java:module") || bindingName.startsWith("java:app")) {
                //this is a binding that does not need to be shared.
                try {
                    final BinderService service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource());
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
                        throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingName, bindingConfiguration.getSource());
                } catch (CircularDependencyException e) {
                    throw EeLogger.ROOT_LOGGER.circularDependency(bindingName);
                }
            } else {
                BinderService service;
                ServiceController<ManagedReferenceFactory> controller;
                try {
                    service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource(), true);
                    ServiceTarget externalServiceTarget = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.EXTERNAL_SERVICE_TARGET);
                    ServiceBuilder<ManagedReferenceFactory> serviceBuilder = externalServiceTarget.addService(bindInfo.getBinderServiceName(), service);
                    bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                    serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                    controller = serviceBuilder.install();
                } catch (DuplicateServiceException e) {
                    controller = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
                    if (controller == null)
                        throw e;
                    service = (BinderService) controller.getService();
                    if (!equals(service.getSource(), bindingConfiguration.getSource())) {
                        throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingName, bindingConfiguration.getSource());
                    }
                }
                //as these bindings are not child services
                //we instead add another service that is a child service that will release the reference count on stop
                //if the reference count hits zero this service will wait till the binding is down to actually stop
                //to prevent WFLY-9870 where the undeployment is complete but the binding services are still present
                try {
                    phaseContext.getServiceTarget().addService(phaseContext.getDeploymentUnit().getServiceName().append("sharedBindingReleaseService").append(bindInfo.getBinderServiceName()), new BinderReleaseService(controller, service))
                            .install();
                    service.acquire();
                } catch (DuplicateServiceException ignore) {
                    //we ignore this, as it is already managed by this deployment
                }
            }
        } else {
            throw EeLogger.ROOT_LOGGER.nullBindingName(bindingConfiguration);
        }
    }

    public static boolean equals(Object one, Object two) {
        return one == two || (one != null && one.equals(two));
    }

    public void undeploy(DeploymentUnit context) {
    }

    private static class BinderReleaseService implements Service<BinderReleaseService> {

        private final ServiceController<ManagedReferenceFactory> sharedBindingController;
        private final BinderService binderService;

        private BinderReleaseService(ServiceController<ManagedReferenceFactory> sharedBindingController, BinderService binderService) {
            this.sharedBindingController = sharedBindingController;
            this.binderService = binderService;
        }

        @Override
        public void start(StartContext context) throws StartException {

        }

        @Override
        public void stop(StopContext context) {
            if(binderService.release()) {
                //we are the last user, it needs to shut down
                context.asynchronous();
                sharedBindingController.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        if(event == LifecycleEvent.REMOVED) {
                            context.complete();
                        }
                    }
                });
            }
        }

        @Override
        public BinderReleaseService getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}
