/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.deployment.TimerServiceResource;
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.TimedObjectInvokerFactoryImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryServiceInstaller;
import org.jboss.as.ejb3.timerservice.TimerServiceMetaData;
import org.jboss.as.ejb3.timerservice.TimerServiceRegistryImpl;
import org.jboss.as.ejb3.timerservice.composite.CompositeTimerServiceFactoryServiceInstaller;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimerServiceFactoryServiceInstaller;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration.TimerFilter;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Deployment processor that sets up the timer service for singletons and stateless session beans
 *
 * NOTE: References in this document to Enterprise JavaBeans (EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
 *
 * @author Stuart Douglas
 */
public class TimerServiceDeploymentProcessor implements DeploymentUnitProcessor {

    private final String threadPoolName;
    private final TimerServiceMetaData defaultMetaData;

    public TimerServiceDeploymentProcessor(final String threadPoolName, final TimerServiceMetaData defaultMetaData) {
        this.threadPoolName = threadPoolName;
        this.defaultMetaData = defaultMetaData;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) return;

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);

        // if this is an EJB deployment then create an EJB module level TimerServiceRegistry which can be used by the timer services
        // of all EJB components that belong to this EJB module.
        final TimerServiceRegistry timerServiceRegistry = new TimerServiceRegistryImpl();

        Map<String, TimerServiceMetaData> timerServiceMetaData = new HashMap<>();
        timerServiceMetaData.put(null, this.defaultMetaData);

        // determine the per-EJB timer persistence service names required
        if (ejbJarMetaData != null && ejbJarMetaData.getAssemblyDescriptor() != null) {
            List<TimerServiceMetaData> timerService = ejbJarMetaData.getAssemblyDescriptor().getAny(TimerServiceMetaData.class);
            if (timerService != null) {
                for (TimerServiceMetaData metaData : timerService) {
                    if ((metaData.getDataStoreName() == null) && (metaData.getPersistentTimerManagementProvider() == null)) {
                        metaData.setDataStoreName(this.defaultMetaData.getDataStoreName());
                        metaData.setPersistentTimerManagementProvider(this.defaultMetaData.getPersistentTimerManagementProvider());
                    }
                    if (metaData.getTransientTimerManagementProvider() == null) {
                        metaData.setTransientTimerManagementProvider(this.defaultMetaData.getTransientTimerManagementProvider());
                    }
                    String name = metaData.getEjbName().equals("*") ? null : metaData.getEjbName();
                    timerServiceMetaData.put(name, metaData);
                }
            }
        }

        String threadPoolName = this.threadPoolName;
        TimerServiceMetaData defaultMetaData = timerServiceMetaData.get(null);

        StringBuilder deploymentNameBuilder = new StringBuilder();
        deploymentNameBuilder.append(moduleDescription.getApplicationName()).append('.').append(moduleDescription.getModuleName());
        String distinctName = moduleDescription.getDistinctName();
        if ((distinctName != null) && !distinctName.isEmpty()) {
            deploymentNameBuilder.append('.').append(distinctName);
        }
        String deploymentName = deploymentNameBuilder.toString();

        TimedObjectInvokerFactory invokerFactory = new TimedObjectInvokerFactoryImpl(module, deploymentName);

        for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {

            // Install per-EJB timer service factories
            if (componentDescription.isTimerServiceApplicable()) {
                ServiceName serviceName = componentDescription.getServiceName().append("timer-service-factory");

                componentDescription.getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) {
                        ROOT_LOGGER.debugf("Installing timer service factory for component %s", componentDescription.getComponentName());
                        EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;
                        TimerServiceResource resource = new TimerServiceResource();
                        ManagedTimerServiceFactoryConfiguration factoryConfiguration = new ManagedTimerServiceFactoryConfiguration() {
                            @Override
                            public TimerServiceRegistry getTimerServiceRegistry() {
                                return timerServiceRegistry;
                            }

                            @Override
                            public TimerListener getTimerListener() {
                                return resource;
                            }

                            @Override
                            public TimedObjectInvokerFactory getInvokerFactory() {
                                return invokerFactory;
                            }
                        };

                        if (componentDescription.isTimerServiceRequired()) {
                            // the component has timeout methods, it needs a 'real' timer service

                            // Only register the TimerService resource if the component requires a TimerService.
                            ejbComponentDescription.setTimerServiceResource(resource);

                            TimerServiceMetaData componentMetaData = timerServiceMetaData.getOrDefault(ejbComponentDescription.getEJBName(), defaultMetaData);

                            if ((threadPoolName != null) && (componentMetaData.getDataStoreName() != null)) {
                                // Install in-memory timer service factory w/persistence support
                                new TimerServiceFactoryServiceInstaller(serviceName, factoryConfiguration, TimerFilter.ALL, threadPoolName, componentMetaData.getDataStoreName()).install(context);
                            } else {
                                // Use composite timer service, with separate transient vs persistent implementations.
                                ServiceName transientServiceName = TimerFilter.TRANSIENT.apply(serviceName);
                                ServiceName persistentServiceName = TimerFilter.PERSISTENT.apply(serviceName);

                                if (componentMetaData.getTransientTimerManagementProvider() != null) {
                                    installDistributableTimerServiceFactory(phaseContext, transientServiceName, componentMetaData.getTransientTimerManagementProvider(), factoryConfiguration, componentDescription, TimerFilter.TRANSIENT);
                                } else {
                                    // Install in-memory timer service factory w/out persistence support
                                    new TimerServiceFactoryServiceInstaller(transientServiceName, factoryConfiguration, TimerFilter.TRANSIENT, threadPoolName, null).install(context);
                                }

                                installDistributableTimerServiceFactory(phaseContext, persistentServiceName, componentMetaData.getPersistentTimerManagementProvider(), factoryConfiguration, componentDescription, TimerFilter.PERSISTENT);

                                new CompositeTimerServiceFactoryServiceInstaller(serviceName, factoryConfiguration).install(phaseContext);
                            }
                        } else {
                            // the EJB is of a type that could have a timer service, but has no timer methods. just bind the non-functional timer service
                            String message = ejbComponentDescription.isStateful() ? EjbLogger.ROOT_LOGGER.timerServiceMethodNotAllowedForSFSB(ejbComponentDescription.getComponentName()) : EjbLogger.ROOT_LOGGER.ejbHasNoTimerMethods();
                            ServiceInstaller.builder(new NonFunctionalTimerServiceFactory(message, factoryConfiguration))
                                    .provides(serviceName)
                                    .build()
                                    .install(context);
                        }

                        configuration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                            @Override
                            public void configureDependency(ServiceBuilder<?> builder, EJBComponentCreateService service) {
                                builder.addDependency(serviceName, ManagedTimerServiceFactory.class, service.getTimerServiceFactoryInjector());
                            }
                        });
                    }
                });
            }
        }
    }

    static void installDistributableTimerServiceFactory(DeploymentPhaseContext context, ServiceName name, String providerName, ManagedTimerServiceFactoryConfiguration factoryConfiguration, ComponentDescription description, TimerFilter filter) {
        DeploymentUnit unit = context.getDeploymentUnit();
        List<String> parts = new ArrayList<>(3);
        if (unit.getParent() != null) {
            parts.add(unit.getParent().getName());
        }
        parts.add(unit.getName());
        parts.add(description.getComponentName());
        parts.add(filter.name());
        String timerServiceName = String.join(".", parts);
        TimerServiceConfiguration configuration = new TimerServiceConfiguration() {
            @Override
            public String getName() {
                return timerServiceName;
            }

            @Override
            public String getDeploymentName() {
                return unit.getName();
            }

            @Override
            public ServiceName getDeploymentServiceName() {
                return unit.getServiceName();
            }

            @Override
            public Module getModule() {
                return unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
            }
        };

        CapabilityServiceSupport support = unit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        ServiceDependency<TimerManagementProvider> provider = ServiceDependency.on(TimerManagementProvider.SERVICE_DESCRIPTOR, providerName);
        ServiceInstaller.builder(new DistributableTimerServiceFactoryServiceInstaller(name, factoryConfiguration, configuration, provider, filter), support)
                .requires(provider)
                .build()
                .install(context);
    }
}
