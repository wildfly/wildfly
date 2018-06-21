/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateless;


import java.lang.reflect.Method;
import java.util.Collection;

import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.session.StatelessRemoteViewInstanceFactory;
import org.jboss.as.ejb3.component.session.StatelessWriteReplaceInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.EjbBMTInterceptor;
import org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor;
import org.jboss.as.ejb3.tx.TimerCMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * User: jpai
 */
public class StatelessComponentDescription extends SessionBeanComponentDescription {

    private String poolConfigName;
    private final boolean defaultSlsbPoolAvailable;

    /**
     * Construct a new instance.
     *
     * @param componentName        the component name
     * @param componentClassName   the component instance class name
     * @param ejbModuleDescription the module description
     */
    public StatelessComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbModuleDescription,
                                         final ServiceName deploymentUnitServiceName, final SessionBeanMetaData descriptorData, final boolean defaultSlsbPoolAvailable) {
        super(componentName, componentClassName, ejbModuleDescription, deploymentUnitServiceName, descriptorData);
        this.defaultSlsbPoolAvailable = defaultSlsbPoolAvailable;
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        final ComponentConfiguration statelessComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
        // setup the component create service
        statelessComponentConfiguration.setComponentCreateServiceFactory(new StatelessComponentCreateServiceFactory());

        // setup the configurator to inject the PoolConfig in the StatelessSessionComponentCreateService
        final StatelessComponentDescription statelessComponentDescription = (StatelessComponentDescription) statelessComponentConfiguration.getComponentDescription();
        statelessComponentConfiguration.getCreateDependencies().add(new PoolInjectingConfigurator(statelessComponentDescription));

        // add the bmt interceptor
        if (TransactionManagementType.BEAN.equals(this.getTransactionManagementType())) {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                }
            });
        }
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                if (TransactionManagementType.CONTAINER.equals(getTransactionManagementType())) {

                    final EEApplicationClasses applicationClasses = context.getDeploymentUnit().getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
                    InterceptorClassDescription interceptorConfig = ComponentDescription.mergeInterceptorConfig(configuration.getComponentClass(), applicationClasses.getClassByName(description.getComponentClassName()), description, MetadataCompleteMarker.isMetadataComplete(context.getDeploymentUnit()));

                    configuration.addPostConstructInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPostConstruct(), true), InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    configuration.addPreDestroyInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPreDestroy(), true), InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);

                    configuration.addTimeoutViewInterceptor(TimerCMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
                }
                configuration.addTimeoutViewInterceptor(StatelessComponentInstanceAssociatingFactory.instance(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });

        return statelessComponentConfiguration;
    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.STATELESS;
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);
        addViewSerializationInterceptor(view);

        // add the instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                //add equals/hashCode interceptor
                //add equals/hashCode interceptor
                for (Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                            method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                    method.getParameterTypes()[0] == Object.class) {
                        configuration.addClientInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
                    }
                }

                // add the stateless component instance associating interceptor
                configuration.addViewInterceptor(StatelessComponentInstanceAssociatingFactory.instance(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });


        if (view instanceof EJBViewDescription) {
            EJBViewDescription ejbViewDescription = (EJBViewDescription) view;
            if (ejbViewDescription.getMethodIntf() == MethodIntf.REMOTE) {
                view.getConfigurators().add(new ViewConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                        final String earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
                        configuration.setViewInstanceFactory(new StatelessRemoteViewInstanceFactory(earApplicationName, componentConfiguration.getModuleName(), componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName(), componentConfiguration.getComponentName()));
                    }
                });
            }
        }

    }

    private void addViewSerializationInterceptor(final ViewDescription view) {
        view.setSerializable(true);
        view.setUseWriteReplace(true);
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final DeploymentReflectionIndex index = context.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
                ClassReflectionIndex classIndex = index.getClassIndex(WriteReplaceInterface.class);
                for (Method method : (Collection<Method>)classIndex.getMethods()) {
                    configuration.addClientInterceptor(method, StatelessWriteReplaceInterceptor.factory(configuration.getViewServiceName().getCanonicalName()), InterceptorOrder.Client.WRITE_REPLACE);
                }
            }
        });
    }

    @Override
    protected ViewConfigurator getSessionBeanObjectViewConfigurator() {
        return StatelessSessionBeanObjectViewConfigurator.INSTANCE;
    }

    boolean isDefaultSlsbPoolAvailable() {
        return defaultSlsbPoolAvailable;
    }

    @Override
    public boolean isTimerServiceApplicable() {
        return true;
    }

    public void setPoolConfigName(final String poolConfigName) {
        this.poolConfigName = poolConfigName;
    }

    public String getPoolConfigName() {
        return this.poolConfigName;
    }

    private static class PoolInjectingConfigurator implements DependencyConfigurator<Service<Component>> {

        private final StatelessComponentDescription statelessComponentDescription;

        PoolInjectingConfigurator(final StatelessComponentDescription statelessComponentDescription) {
            this.statelessComponentDescription = statelessComponentDescription;
        }

        @Override
        public void configureDependency(ServiceBuilder<?> serviceBuilder, Service<Component> service) throws DeploymentUnitProcessingException {
            final StatelessSessionComponentCreateService statelessSessionComponentService = (StatelessSessionComponentCreateService) service;
            final String poolName = this.statelessComponentDescription.getPoolConfigName();
            // if no pool name has been explicitly set, then inject the *optional* "default slsb pool config".
            // If the default slsb pool config itself is not configured, then the pooling is disabled for the bean
            if (poolName == null) {
                if (statelessComponentDescription.isDefaultSlsbPoolAvailable()) {
                    serviceBuilder.addDependency(StrictMaxPoolConfigService.DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME,
                            PoolConfig.class, statelessSessionComponentService.getPoolConfigInjector());
                }
            } else {
                // pool name has been explicitly set so the pool config is a required dependency
                serviceBuilder.addDependency(StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName),
                        PoolConfig.class, statelessSessionComponentService.getPoolConfigInjector());
            }
        }
    }
}
