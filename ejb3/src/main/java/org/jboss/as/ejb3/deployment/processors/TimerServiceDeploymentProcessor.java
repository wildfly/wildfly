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
package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.TimedObjectInvokerFactoryImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.TimerServiceMetaData;
import org.jboss.as.ejb3.timerservice.TimerServiceRegistryImpl;
import org.jboss.as.ejb3.timerservice.composite.CompositeTimerServiceFactoryServiceConfigurator;
import org.jboss.as.ejb3.timerservice.distributable.DistributableTimerServiceFactoryServiceConfigurator;
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
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.BeanConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerServiceRequirement;
import org.wildfly.clustering.service.ChildTargetService;

/**
 * Deployment processor that sets up the timer service for singletons and stateless session beans
 *
 * NOTE: References in this document to Enterprise JavaBeans(EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
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
        BeanConfiguration beanConfiguration = new BeanConfiguration() {
            @Override
            public String getName() {
                List<String> parts = new ArrayList<>(3);
                if (unit.getParent() != null) {
                    parts.add(unit.getParent().getName());
                }
                parts.add(unit.getName());
                parts.add(description.getComponentName());
                parts.add(filter.name());
                return String.join(".", parts);
            }

            @Override
            public ServiceName getDeploymentUnitServiceName() {
                return unit.getServiceName();
            }

            @Override
            public Module getModule() {
                return unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
            }
        };

        CapabilityServiceSupport support = unit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        ServiceName providerServiceName = TimerServiceRequirement.TIMER_MANAGEMENT_PROVIDER.getServiceName(support, providerName);
        ServiceBuilder<?> builder = context.getServiceTarget().addService(name.append("installer"));
        Supplier<TimerManagementProvider> dependency = builder.requires(providerServiceName);
        builder.setInstance(new ChildTargetService(new Consumer<ServiceTarget>() {
            @Override
            public void accept(ServiceTarget target) {
                TimerManagementProvider provider = dependency.get();
                new DistributableTimerServiceFactoryServiceConfigurator(name, factoryConfiguration, beanConfiguration, provider, filter).configure(support).build(target).install();
            }
        })).install();
    }
}
