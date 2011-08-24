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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.session.SessionInvocationContextInterceptor;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.timerservice.AutoTimer;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryService;
import org.jboss.as.ejb3.timerservice.TimerServiceService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.txn.TransactionManagerService;
import org.jboss.as.txn.TransactionSynchronizationRegistryService;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceFactory;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.ejb.Schedule;
import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Deployment processor that sets up the timer service for singletons and stateless session beans
 *
 * @author Stuart Douglas
 */
public class TimerServiceDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(TimerServiceDeploymentProcessor.class);

    private final int coreThreads;
    private final int maxThreads;

    private final boolean enabled;

    public TimerServiceDeploymentProcessor(final int coreThreads, final int maxThreads, boolean enabled) {
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.enabled = enabled;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (!enabled) {
            return;
        }
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        boolean timerServiceRequired = false;

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {

            if (component instanceof SingletonComponentDescription || component instanceof StatelessComponentDescription) {
                timerServiceRequired = true;
                logger.debug("Installing timer service for component " + component.getComponentName());

                component.getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                        final SessionBeanComponentDescription ejbComponentDescription = (SessionBeanComponentDescription) description;

                        final DeploymentReflectionIndex deploymentReflectionIndex = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
                        final EEApplicationClasses applicationClasses = phaseContext.getDeploymentUnit().getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

                        final RuntimeAnnotationInformation<AutoTimer> scheduleAnnotationData = MethodAnnotationAggregator.runtimeAnnotationInformation(configuration.getComponentClass(), applicationClasses, deploymentReflectionIndex, Schedule.class);
                        final Set<Method> timerAnnotationData = MethodAnnotationAggregator.runtimeAnnotationPresent(configuration.getComponentClass(), applicationClasses, deploymentReflectionIndex, Timeout.class);
                        final Method timeoutMethod;
                        if(timerAnnotationData.size() > 1) {
                            throw new DeploymentUnitProcessingException("Component class " + configuration.getComponentClass() + " has multiple @Timeout annotations");
                        } else if(timerAnnotationData.size() == 1) {
                            timeoutMethod = timerAnnotationData.iterator().next();
                        } else {
                            timeoutMethod = null;
                        }

                        //First resolve the timer method and auto timer
                        Class<?> c = configuration.getComponentClass();
                        while (c != null && c != Object.class) {
                            final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(c);
                            //TimedObject takes precedence
                            Method method = null;
                            if (TimedObject.class.isAssignableFrom(configuration.getComponentClass())) {
                                method = index.getMethod(Void.TYPE, "ejbTimeout", javax.ejb.Timer.class);
                            } else if (ejbComponentDescription.getTimeoutMethod() == null && timeoutMethod != null) {
                                method = timeoutMethod;
                            } else {
                                break;
                            }
                            if (method != null) {
                                ejbComponentDescription.setTimeoutMethod(method);
                                break;
                            }
                            c = c.getSuperclass();
                        }
                        //now for the schedule methods
                        for (Map.Entry<Method, List<AutoTimer>> entry : scheduleAnnotationData.getMethodAnnotations().entrySet()) {

                            for (AutoTimer timer : entry.getValue()) {
                                ejbComponentDescription.addScheduleMethod(entry.getKey(), timer);
                            }
                        }

                        configuration.addTimeoutInterceptor(SessionInvocationContextInterceptor.FACTORY, InterceptorOrder.Component.TIMEOUT_INVOCATION_CONTEXT_INTERCEPTOR);

                        //install the timer create service
                        final TimerServiceService service = new TimerServiceService(ejbComponentDescription.getScheduleMethods(), module.getClassLoader());
                        final ServiceName serviceName = component.getServiceName().append(TimerServiceService.SERVICE_NAME);
                        final ServiceBuilder<javax.ejb.TimerService> createBuilder = context.getServiceTarget().addService(serviceName, service);
                        createBuilder.addDependency(deploymentUnit.getServiceName().append(TimerServiceFactoryService.SERVICE_NAME), TimerServiceFactory.class, service.getTimerServiceFactoryInjectedValue());
                        createBuilder.addDependency(component.getCreateServiceName(), EJBComponent.class, service.getEjbComponentInjectedValue());
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
        if (timerServiceRequired) {
            addTimerService(phaseContext.getServiceTarget(), deploymentUnit, module);
        }
    }


    /**
     * Adds a service that creates the entity manager factory used by the timer service, and the
     * timer service factory service
     *
     * @param serviceTarget The service target to add the service to
     */
    private void addTimerService(final ServiceTarget serviceTarget, final DeploymentUnit deploymentUnit, final Module module) {


        final String name;
        if (deploymentUnit.getParent() == null) {
            name = deploymentUnit.getName();
        } else {
            name = deploymentUnit.getParent().getName() + "--" + deploymentUnit.getName();
        }

        final TimerServiceFactoryService factoryService = new TimerServiceFactoryService(coreThreads, maxThreads, name, module);
        final ServiceBuilder<TimerServiceFactory> factoryBuilder = serviceTarget.addService(deploymentUnit.getServiceName().append(TimerServiceFactoryService.SERVICE_NAME), factoryService);
        factoryBuilder.addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, factoryService.getTransactionManagerInjectedValue());
        factoryBuilder.addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, factoryService.getTransactionSynchronizationRegistryInjectedValue());
        factoryBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TimerServiceFactoryService.PATH_SERVICE_NAME, String.class, factoryService.getPath());
        factoryBuilder.install();
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
