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
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.deployment.TimerServiceResource;
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.TimedObjectInvokerFactoryImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.TimerServiceMetaData;
import org.jboss.as.ejb3.timerservice.TimerServiceRegistryImpl;
import org.jboss.as.ejb3.timerservice.composite.CompositeTimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimeoutListener;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimerService;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimerServiceConfiguration;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimerSynchronizationFactory;
import org.jboss.as.ejb3.timerservice.distributable.TimerSynchronizationFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration.TimerFilter;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerManagerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.ejb.timer.TimerServiceConfiguration;
import org.wildfly.clustering.server.util.UUIDFactory;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import jakarta.ejb.TimerConfig;

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

        // support for using capabilities to resolve service names
        CapabilityServiceSupport capabilityServiceSupport = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);

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
                        ServiceTarget target = context.getServiceTarget();
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
                                new TimerServiceFactoryServiceConfigurator(serviceName, factoryConfiguration, threadPoolName, componentMetaData.getDataStoreName()).configure(capabilityServiceSupport).build(target).install();
                            } else {
                                // Use composite timer service, with separate transient vs persistent implementations.
                                ServiceName transientServiceName = TimerFilter.TRANSIENT.apply(serviceName);
                                ServiceName persistentServiceName = TimerFilter.PERSISTENT.apply(serviceName);

                                if (componentMetaData.getTransientTimerManagementProvider() != null) {
                                    installDistributableTimerServiceFactory(phaseContext, transientServiceName, componentMetaData.getTransientTimerManagementProvider(), factoryConfiguration, componentDescription, TimerFilter.TRANSIENT);
                                } else {
                                    // Install in-memory timer service factory w/out persistence support
                                    new TimerServiceFactoryServiceConfigurator(transientServiceName, factoryConfiguration, threadPoolName, null).filter(TimerFilter.TRANSIENT).configure(capabilityServiceSupport).build(target).install();
                                }

                                installDistributableTimerServiceFactory(phaseContext, persistentServiceName, componentMetaData.getPersistentTimerManagementProvider(), factoryConfiguration, componentDescription, TimerFilter.PERSISTENT);

                                new CompositeTimerServiceFactoryServiceConfigurator(serviceName, factoryConfiguration).build(target).install();
                            }
                        } else {
                            // the EJB is of a type that could have a timer service, but has no timer methods. just bind the non-functional timer service

                            String message = ejbComponentDescription.isStateful() ? EjbLogger.ROOT_LOGGER.timerServiceMethodNotAllowedForSFSB(ejbComponentDescription.getComponentName()) : EjbLogger.ROOT_LOGGER.ejbHasNoTimerMethods();
                            new NonFunctionalTimerServiceFactoryServiceConfigurator(serviceName, message, factoryConfiguration).build(target).install();
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
        TimerListener timerListener = factoryConfiguration.getTimerListener();
        TimerRegistry<UUID> timerRegistry = new TimerRegistry<>() {
            @Override
            public void register(UUID id) {
                timerListener.timerAdded(id.toString());
            }

            @Override
            public void unregister(UUID id) {
                timerListener.timerRemoved(id.toString());
            }
        };
        TimerManagerFactoryConfiguration<UUID> managerFactoryConfiguration = new TimerManagerFactoryConfiguration<>() {
            @Override
            public Supplier<UUID> getIdentifierFactory() {
                return TimerIdentifierFactory.INSTANCE;
            }

            @Override
            public TimerServiceConfiguration getTimerServiceConfiguration() {
                return configuration;
            }

            @Override
            public TimerRegistry<UUID> getRegistry() {
                return timerRegistry;
            }

            @Override
            public boolean isPersistent() {
                return filter.test(new TimerConfig(null, true));
            }
        };
        ServiceName timerManagerFactoryName = ServiceName.JBOSS.append("clustering", "timer", timerServiceName);
        TimedObjectInvokerFactory invokerFactory = factoryConfiguration.getInvokerFactory();
        TimerServiceRegistry registry = factoryConfiguration.getTimerServiceRegistry();
        ServiceDependency<TimerManagerFactory<UUID>> managerFactory = ServiceDependency.on(timerManagerFactoryName);
        ManagedTimerServiceFactory factory = new ManagedTimerServiceFactory() {
            @Override
            public ManagedTimerService createTimerService(EJBComponent component) {
                TimedObjectInvoker invoker = invokerFactory.createInvoker(component);
                TimerSynchronizationFactory<UUID> synchronizationFactory = new DistributableTimerSynchronizationFactory<>(timerRegistry);
                TimeoutListener<UUID> timeoutListener = new DistributableTimeoutListener<>(invoker, synchronizationFactory);
                TimerManager<UUID> manager = managerFactory.get().createTimerManager(new TimerManagerConfiguration<UUID>() {
                    @Override
                    public TimerServiceConfiguration getTimerServiceConfiguration() {
                        return managerFactoryConfiguration.getTimerServiceConfiguration();
                    }

                    @Override
                    public Supplier<UUID> getIdentifierFactory() {
                        return managerFactoryConfiguration.getIdentifierFactory();
                    }

                    @Override
                    public TimerRegistry<UUID> getRegistry() {
                        return managerFactoryConfiguration.getRegistry();
                    }

                    @Override
                    public boolean isPersistent() {
                        return managerFactoryConfiguration.isPersistent();
                    }

                    @Override
                    public TimeoutListener<UUID> getListener() {
                        return timeoutListener;
                    }
                });
                DistributableTimerServiceConfiguration<UUID> serviceConfiguration = new DistributableTimerServiceConfiguration<>() {
                    @Override
                    public TimedObjectInvoker getInvoker() {
                        return invoker;
                    }

                    @Override
                    public TimerServiceRegistry getTimerServiceRegistry() {
                        return registry;
                    }

                    @Override
                    public TimerListener getTimerListener() {
                        return timerListener;
                    }

                    @Override
                    public Function<String, UUID> getIdentifierParser() {
                        return TimerIdentifierFactory.INSTANCE;
                    }

                    @Override
                    public Predicate<TimerConfig> getTimerFilter() {
                        return filter;
                    }

                    @Override
                    public TimerSynchronizationFactory<UUID> getTimerSynchronizationFactory() {
                        return synchronizationFactory;
                    }
                };
                return new DistributableTimerService<>(serviceConfiguration, manager);
            }
        };
        ServiceInstaller.builder(factory).provides(name).requires(managerFactory).build().install(context);

        ServiceDependency<TimerManagementProvider> provider = ServiceDependency.on(TimerManagementProvider.SERVICE_DESCRIPTOR, providerName);
        ServiceInstaller installer = new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                for (ServiceInstaller installer : provider.get().getTimerManagerFactoryServiceInstallers(timerManagerFactoryName, managerFactoryConfiguration)) {
                    installer.install(target);
                }
                return null;
            }
        };
        ServiceInstaller.builder(installer, unit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT)).requires(provider).build().install(context);
    }

    enum TimerIdentifierFactory implements Supplier<java.util.UUID>, Function<String, UUID> {
        INSTANCE;

        @Override
        public java.util.UUID get() {
            return UUIDFactory.INSECURE.get();
        }

        @Override
        public java.util.UUID apply(String id) {
            return java.util.UUID.fromString(id);
        }
    }
}
