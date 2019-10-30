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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ejb.Asynchronous;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.interceptors.AsyncFutureInterceptorFactory;
import org.jboss.as.ejb3.component.interceptors.LogDiagnosticContextRecoveryInterceptor;
import org.jboss.as.ejb3.component.interceptors.LogDiagnosticContextStorageInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.ejb3.security.SecurityDomainInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AsyncMethodMetaData;
import org.jboss.metadata.ejb.spec.AsyncMethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Merging processor that handles EJB async methods, and adds a configurator to configure any that are found.
 *
 * @author Stuart Douglas
 */
public class AsynchronousMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription> {

    final ServiceName asynchronousThreadPoolService;

    public AsynchronousMergingProcessor(final ServiceName asynchronousThreadPoolService) {
        super(SessionBeanComponentDescription.class);
        this.asynchronousThreadPoolService = asynchronousThreadPoolService;
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        final RuntimeAnnotationInformation<Boolean> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Asynchronous.class);
        for (final Map.Entry<String, List<Boolean>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                description.addAsynchronousClass(entry.getKey());
            }
        }

        for (final Map.Entry<Method, List<Boolean>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                description.addAsynchronousMethod(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
            }
        }

    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        final SessionBeanMetaData data = description.getDescriptorData();
        final boolean isSecurityDomainKnown = description.isSecurityDomainKnown();
        if (data != null) {
            if (data instanceof SessionBean31MetaData) {
                final SessionBean31MetaData sessionBeanData = (SessionBean31MetaData) data;
                final AsyncMethodsMetaData async = sessionBeanData.getAsyncMethods();
                if (async != null) {
                    for (AsyncMethodMetaData method : async) {
                        final Collection<Method> methods = MethodResolutionUtils.resolveMethods(method.getMethodName(), method.getMethodParams(), componentClass, deploymentReflectionIndex);
                        for(final Method m : methods ) {
                            description.addAsynchronousMethod(MethodIdentifier.getIdentifierForMethod(m));
                        }
                    }
                }
            }
        }
        if (!description.getAsynchronousClasses().isEmpty() ||
                !description.getAsynchronousMethods().isEmpty()) {

            //setup a dependency on the executor service
            description.getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    configuration.getCreateDependencies().add(new DependencyConfigurator<SessionBeanComponentCreateService>() {
                        @Override
                        public void configureDependency(final ServiceBuilder<?> serviceBuilder, final SessionBeanComponentCreateService service) throws DeploymentUnitProcessingException {
                            serviceBuilder.addDependency(asynchronousThreadPoolService, ExecutorService.class, service.getAsyncExecutorService());
                        }
                    });
                }
            });
            for (final ViewDescription view : description.getViews()) {
                final EJBViewDescription ejbView = (EJBViewDescription) view;

                ejbView.getConfigurators().add(new ViewConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                        final SessionBeanComponentDescription componentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
                        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {

                            //we need the component method to get the correct declaring class
                            final Method componentMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, componentClass, method);

                            if (componentMethod != null) {
                                if (componentDescription.getAsynchronousClasses().contains(componentMethod.getDeclaringClass().getName())) {
                                    addAsyncInterceptor(configuration, method, isSecurityDomainKnown);
                                    configuration.addAsyncMethod(method);
                                } else {
                                    MethodIdentifier id = MethodIdentifier.getIdentifierForMethod(method);
                                    if (componentDescription.getAsynchronousMethods().contains(id)) {
                                        addAsyncInterceptor(configuration, method, isSecurityDomainKnown);
                                        configuration.addAsyncMethod(method);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private static void addAsyncInterceptor(final ViewConfiguration configuration, final Method method, final boolean isSecurityDomainKnown) throws DeploymentUnitProcessingException {
        if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Future.class)) {
            configuration.addClientInterceptor(method, LogDiagnosticContextStorageInterceptor.getFactory(), InterceptorOrder.Client.LOCAL_ASYNC_LOG_SAVE);

            if (isSecurityDomainKnown) {
                // Make sure the security domain is available in the private data of the InterceptorContext
                configuration.addClientInterceptor(method, SecurityDomainInterceptorFactory.INSTANCE, InterceptorOrder.Client.LOCAL_ASYNC_SECURITY_CONTEXT);
            }
            configuration.addClientInterceptor(method, AsyncFutureInterceptorFactory.INSTANCE, InterceptorOrder.Client.LOCAL_ASYNC_INVOCATION);

            configuration.addClientInterceptor(method, LogDiagnosticContextRecoveryInterceptor.getFactory(), InterceptorOrder.Client.LOCAL_ASYNC_LOG_RESTORE);
        } else {
            throw EjbLogger.ROOT_LOGGER.wrongReturnTypeForAsyncMethod(method);
        }
    }
}
