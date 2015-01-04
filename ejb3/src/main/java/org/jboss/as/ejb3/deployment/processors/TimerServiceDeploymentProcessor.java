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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.TimerServiceRegistry;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerService;
import org.jboss.as.ejb3.timerservice.TimedObjectInvokerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceMetaData;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * Deployment processor that sets up the timer service for singletons and stateless session beans
 *
 * @author Stuart Douglas
 */
public class TimerServiceDeploymentProcessor implements DeploymentUnitProcessor {

    public static final ServiceName TIMER_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timer");

    private final ServiceName timerServiceThreadPool;
    private final String defaultTimerDataStore;

    public TimerServiceDeploymentProcessor(final ServiceName timerServiceThreadPool, final String defaultTimerDataStore) {
        this.timerServiceThreadPool = timerServiceThreadPool;
        this.defaultTimerDataStore = defaultTimerDataStore;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);

        ServiceName defaultTimerPersistenceService = TimerPersistence.SERVICE_NAME.append(defaultTimerDataStore);
        final Map<String, ServiceName> timerPersistenceServices = new HashMap<String, ServiceName>();
        // if this is an EJB deployment then create an EJB module level TimerServiceRegistry which can be used by the timer services
        // of all EJB components that belong to this EJB module.
        final TimerServiceRegistry timerServiceRegistry = EjbDeploymentMarker.isEjbDeployment(deploymentUnit) ? new TimerServiceRegistry() : null;

        if (ejbJarMetaData != null && ejbJarMetaData.getAssemblyDescriptor() != null) {
            List<TimerServiceMetaData> timerService = ejbJarMetaData.getAssemblyDescriptor().getAny(TimerServiceMetaData.class);
            if (timerService != null) {
                for (TimerServiceMetaData data : timerService) {
                    if (data.getEjbName().equals("*")) {
                        defaultTimerPersistenceService = TimerPersistence.SERVICE_NAME.append(data.getDataStoreName());
                    } else {
                        timerPersistenceServices.put(data.getEjbName(), TimerPersistence.SERVICE_NAME.append(data.getDataStoreName()));
                    }
                }
            }
        }
        final ServiceName finalDefaultTimerPersistenceService = defaultTimerPersistenceService;

        for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {

            if (componentDescription.isTimerServiceApplicable()) {
                if(componentDescription.isTimerServiceRequired()) {
                    //the component has timeout methods, it needs a 'real' timer service
                    final String deploymentName;
                    if (moduleDescription.getDistinctName() == null || moduleDescription.getDistinctName().length() == 0) {
                        deploymentName = moduleDescription.getApplicationName() + "." + moduleDescription.getModuleName();
                    } else {
                        deploymentName = moduleDescription.getApplicationName() + "." + moduleDescription.getModuleName() + "." + moduleDescription.getDistinctName();
                    }

                    ROOT_LOGGER.debugf("Installing timer service for component %s", componentDescription.getComponentName());
                    componentDescription.getConfigurators().add(new ComponentConfigurator() {
                        @Override
                        public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                            final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;

                            final ServiceName invokerServiceName = ejbComponentDescription.getServiceName().append(TimedObjectInvokerImpl.SERVICE_NAME);
                            final TimedObjectInvokerImpl invoker = new TimedObjectInvokerImpl(deploymentName, module);
                            context.getServiceTarget().addService(invokerServiceName, invoker)
                                    .addDependency(componentDescription.getCreateServiceName(), EJBComponent.class, invoker.getEjbComponent())
                                    .install();


                            //install the timer create service
                            final ServiceName serviceName = componentDescription.getServiceName().append(TimerServiceImpl.SERVICE_NAME);
                            final TimerServiceImpl service = new TimerServiceImpl(ejbComponentDescription.getScheduleMethods(), serviceName, timerServiceRegistry);
                            final ServiceBuilder<javax.ejb.TimerService> createBuilder = context.getServiceTarget().addService(serviceName, service);
                            createBuilder.addDependency(TIMER_SERVICE_NAME, Timer.class, service.getTimerInjectedValue());
                            createBuilder.addDependency(componentDescription.getCreateServiceName(), EJBComponent.class, service.getEjbComponentInjectedValue());
                            createBuilder.addDependency(timerServiceThreadPool, ExecutorService.class, service.getExecutorServiceInjectedValue());
                            if (timerPersistenceServices.containsKey(ejbComponentDescription.getEJBName())) {
                                createBuilder.addDependency(timerPersistenceServices.get(ejbComponentDescription.getEJBName()), TimerPersistence.class, service.getTimerPersistence());
                            } else {
                                createBuilder.addDependency(finalDefaultTimerPersistenceService, TimerPersistence.class, service.getTimerPersistence());
                            }
                            createBuilder.addDependency(invokerServiceName, TimedObjectInvoker.class, service.getTimedObjectInvoker());
                            createBuilder.install();
                            ejbComponentDescription.setTimerService(service);
                            //inject the timer service directly into the start service
                            configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                                @Override
                                public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                                    serviceBuilder.addDependency(serviceName);
                                }
                            });
                        }
                    });
                } else {
                    //the EJB is of a type that could have a timer service, but has no timer methods.
                    //just bind the non-functional timer service
                    componentDescription.getConfigurators().add(new ComponentConfigurator() {
                        @Override
                        public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                            final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;
                            final ServiceName nonFunctionalTimerServiceName = NonFunctionalTimerService.serviceNameFor(ejbComponentDescription);
                            final NonFunctionalTimerService nonFunctionalTimerService;
                            if (ejbComponentDescription instanceof StatefulComponentDescription) {
                                // for stateful beans, use a different error message that gets thrown from the NonFunctionalTimerService
                                nonFunctionalTimerService = new NonFunctionalTimerService(EjbLogger.ROOT_LOGGER.timerServiceMethodNotAllowedForSFSB(ejbComponentDescription.getComponentName()), timerServiceRegistry);
                            } else {
                                nonFunctionalTimerService = new NonFunctionalTimerService(EjbLogger.ROOT_LOGGER.ejbHasNoTimerMethods(), timerServiceRegistry);
                            }
                            // add the non-functional timer service as a MSC service
                            context.getServiceTarget().addService(nonFunctionalTimerServiceName, nonFunctionalTimerService).install();
                            // set the timer service in the EJB component
                            ejbComponentDescription.setTimerService(nonFunctionalTimerService);
                            // now we want the EJB component to depend on this non-functional timer service to start
                            configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                                @Override
                                public void configureDependency(ServiceBuilder<?> serviceBuilder, ComponentStartService service) throws DeploymentUnitProcessingException {
                                    serviceBuilder.addDependency(nonFunctionalTimerServiceName);
                                }
                            });
                        }
                    });
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
