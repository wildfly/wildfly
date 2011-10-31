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

import java.util.Timer;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.persistence.filestore.FileTimerPersistence;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;

/**
 * Deployment processor that sets up the timer service for singletons and stateless session beans
 *
 * @author Stuart Douglas
 */
public class TimerServiceDeploymentProcessor implements DeploymentUnitProcessor {

    public static final ServiceName TIMER_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timer");

    public static final ServiceName PATH_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timerService", "dataPath");

    private final ServiceName timerServiceThreadPool;

    public TimerServiceDeploymentProcessor(final ServiceName timerServiceThreadPool) {
        this.timerServiceThreadPool = timerServiceThreadPool;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {

            if (component.isTimerServiceApplicable()) {
                ROOT_LOGGER.debug("Installing timer service for component " + component.getComponentName());

                component.getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;
                        configuration.addTimeoutInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.Component.TIMEOUT_INVOCATION_CONTEXT_INTERCEPTOR);
                        //install the timer create service
                        final ServiceName serviceName = component.getServiceName().append(TimerServiceImpl.SERVICE_NAME);
                        final TimerServiceImpl service = new TimerServiceImpl(ejbComponentDescription.getScheduleMethods(), serviceName);
                        final ServiceBuilder<javax.ejb.TimerService> createBuilder = context.getServiceTarget().addService(serviceName, service);
                        createBuilder.addDependency(TIMER_SERVICE_NAME, Timer.class, service.getTimerInjectedValue());
                        createBuilder.addDependency(component.getCreateServiceName(), EJBComponent.class, service.getEjbComponentInjectedValue());
                        createBuilder.addDependency(timerServiceThreadPool, ExecutorService.class, service.getExecutorServiceInjectedValue());
                        createBuilder.addDependency(FileTimerPersistence.SERVICE_NAME, TimerPersistence.class, service.getTimerPersistence());
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

            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
