/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.messagedriven;


import java.util.Properties;

import javax.ejb.MessageDrivenBean;
import javax.ejb.TransactionManagementType;
import javax.resource.spi.ResourceAdapter;

import org.jboss.as.connector.util.ConnectorServices;
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
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptor;
import org.jboss.as.ejb3.tx.EjbBMTInterceptor;
import org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor;
import org.jboss.as.ejb3.tx.TimerCMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentDescription extends EJBComponentDescription {
    private final Properties activationProps;
    private String resourceAdapterName;
    private boolean deliveryActive;
    private String deliveryGroup;
    private boolean clusteredSingleton;
    private String mdbPoolConfigName;
    private final String messageListenerInterfaceName;
    private final boolean defaultMdbPoolAvailable;

    /**
     * Construct a new instance.
     *
     * @param componentName              the component name
     * @param componentClassName         the component instance class name
     * @param ejbJarDescription          the module description
     * @param defaultResourceAdapterName The default resource adapter name for this message driven bean. Cannot be null or empty string.
     */
    public MessageDrivenComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription,
                                             final ServiceName deploymentUnitServiceName, final String messageListenerInterfaceName, final Properties activationProps,
                                             final String defaultResourceAdapterName, final MessageDrivenBeanMetaData descriptorData, final boolean defaultMdbPoolAvailable) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName, descriptorData);
        if (messageListenerInterfaceName == null || messageListenerInterfaceName.isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.stringParamCannotBeNullOrEmpty("Message listener interface");
        }
        if (defaultResourceAdapterName == null || defaultResourceAdapterName.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.stringParamCannotBeNullOrEmpty("Default resource adapter name");
        }
        this.resourceAdapterName = defaultResourceAdapterName;
        this.deliveryActive = true;
        this.activationProps = activationProps;
        this.messageListenerInterfaceName = messageListenerInterfaceName;
        this.defaultMdbPoolAvailable = defaultMdbPoolAvailable;
        // setup a dependency on the EJBUtilities service
        this.addDependency(EJBUtilities.SERVICE_NAME);

        registerView(getEJBClassName(), MethodIntf.MESSAGE_ENDPOINT);
        // add the interceptor which will invoke the setMessageDrivenContext() method on a MDB which implements
        // MessageDrivenBean interface
        this.addSetMessageDrivenContextMethodInvocationInterceptor();
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addTimeoutViewInterceptor(MessageDrivenComponentInstanceAssociatingFactory.instance(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        final ComponentConfiguration mdbComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
        // setup the component create service
        final Class<?> messageListenerInterface;
        try {
            messageListenerInterface = Class.forName(getMessageListenerInterfaceName(), true, moduleClassLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        mdbComponentConfiguration.setComponentCreateServiceFactory(new MessageDrivenComponentCreateServiceFactory(messageListenerInterface));

        // setup the configurator to inject the PoolConfig in the MessageDrivenComponentCreateService
        final MessageDrivenComponentDescription mdbComponentDescription = (MessageDrivenComponentDescription) mdbComponentConfiguration.getComponentDescription();
        mdbComponentConfiguration.getCreateDependencies().add(new PoolInjectingConfigurator(mdbComponentDescription));

        // setup the configurator to inject the resource adapter
        mdbComponentConfiguration.getCreateDependencies().add(new ResourceAdapterInjectingConfiguration());

        mdbComponentConfiguration.getCreateDependencies().add(new DependencyConfigurator<MessageDrivenComponentCreateService>() {
            @Override
            public void configureDependency(final ServiceBuilder<?> serviceBuilder, final MessageDrivenComponentCreateService mdbComponentCreateService) throws DeploymentUnitProcessingException {
                serviceBuilder.addDependency(EJBUtilities.SERVICE_NAME, EJBUtilities.class, mdbComponentCreateService.getEJBUtilitiesInjector());
                serviceBuilder.addDependency(SuspendController.SERVICE_NAME, SuspendController.class, mdbComponentCreateService.getSuspendControllerInjectedValue());
            }
        });

        // add the bmt interceptor
        if (TransactionManagementType.BEAN.equals(this.getTransactionManagementType())) {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                }
            });
        } else {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final EEApplicationClasses applicationClasses = context.getDeploymentUnit().getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
                    InterceptorClassDescription interceptorConfig = ComponentDescription.mergeInterceptorConfig(configuration.getComponentClass(), applicationClasses.getClassByName(description.getComponentClassName()), description, MetadataCompleteMarker.isMetadataComplete(context.getDeploymentUnit()));

                    configuration.addPostConstructInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPostConstruct(), true), InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    configuration.addPreDestroyInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPreDestroy(), true), InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);

                    configuration.addTimeoutViewInterceptor(TimerCMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
                }
            });
        }


        return mdbComponentConfiguration;
    }

    boolean isDefaultMdbPoolAvailable() {
        return defaultMdbPoolAvailable;
    }

    public Properties getActivationProps() {
        return activationProps;
    }

    public boolean isDeliveryActive() {
        return deliveryActive;
    }

    public void setDeliveryActive(boolean deliveryActive) {
        this.deliveryActive = deliveryActive;
    }

    public String getDeliveryGroup() {
        return deliveryGroup;
    }

    public void setDeliveryGroup(String groupName) {
        this.deliveryGroup = groupName;
    }

    public boolean isClusteredSingleton() {
        return clusteredSingleton;
    }

    public void setClusteredSingleton(boolean clusteredSingleton) {
        this.clusteredSingleton = clusteredSingleton;
    }

    public boolean isDeliveryControlled() {
        return deliveryGroup != null || clusteredSingleton;
    }

    public ServiceName getDeliveryControllerName() {
        return getServiceName().append("DELIVERY");
    }

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        if (resourceAdapterName == null || resourceAdapterName.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.stringParamCannotBeNullOrEmpty("Resource adapter name");
        }
        this.resourceAdapterName = resourceAdapterName;
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let the super do its job
        super.setupViewInterceptors(view);

        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                //add the invocation type to the start of the chain
                //TODO: is there a cleaner way to do this?
                configuration.addViewInterceptor(new ImmediateInterceptorFactory(new Interceptor() {
                    @Override
                    public Object processInvocation(final InterceptorContext context) throws Exception {
                        context.putPrivateData(InvocationType.class, InvocationType.MESSAGE_DELIVERY);
                        return context.proceed();
                    }
                }), InterceptorOrder.View.INVOCATION_TYPE);

                // add the instance associating interceptor at the start of the interceptor chain
                configuration.addViewInterceptor(MessageDrivenComponentInstanceAssociatingFactory.instance(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);

                final MessageDrivenComponentDescription mdb = (MessageDrivenComponentDescription) componentConfiguration.getComponentDescription();
                if (mdb.getTransactionManagementType() == TransactionManagementType.CONTAINER) {
                    configuration.addViewInterceptor(CMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
                }
            }
        });

    }

    @Override
    protected void addCurrentInvocationContextFactory() {
        // add the current invocation context interceptor at the beginning of the component instance post construct chain
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
                configuration.addPreDestroyInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
            }
        });
    }

    @Override
    protected void addCurrentInvocationContextFactory(ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
            }
        });
    }

    /**
     * Adds an interceptor to invoke the {@link MessageDrivenBean#setMessageDrivenContext(javax.ejb.MessageDrivenContext)}
     * if the MDB implements the {@link MessageDrivenBean} interface
     */
    private void addSetMessageDrivenContextMethodInvocationInterceptor() {
        // add the setMessageDrivenContext(MessageDrivenContext) method invocation interceptor for MDB
        // implementing the javax.ejb.MessageDrivenBean interface
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                if (MessageDrivenBean.class.isAssignableFrom(configuration.getComponentClass())) {
                    configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(MessageDrivenBeanSetMessageDrivenContextInterceptor.INSTANCE), InterceptorOrder.ComponentPostConstruct.EJB_SET_CONTEXT_METHOD_INVOCATION_INTERCEPTOR);
                }
            }
        });
    }

    @Override
    public boolean isMessageDriven() {
        return true;
    }

    public void setPoolConfigName(final String mdbPoolConfigName) {
        this.mdbPoolConfigName = mdbPoolConfigName;
    }

    public String getPoolConfigName() {
        return this.mdbPoolConfigName;
    }

    private String getMessageListenerInterfaceName() {
        return messageListenerInterfaceName;
    }

    private static class PoolInjectingConfigurator implements DependencyConfigurator<Service<Component>> {

        private final MessageDrivenComponentDescription mdbComponentDescription;

        PoolInjectingConfigurator(final MessageDrivenComponentDescription mdbComponentDescription) {
            this.mdbComponentDescription = mdbComponentDescription;
        }

        @Override
        public void configureDependency(ServiceBuilder<?> serviceBuilder, Service<Component> service) {
            final MessageDrivenComponentCreateService mdbComponentCreateService = (MessageDrivenComponentCreateService) service;
            final String poolName = this.mdbComponentDescription.getPoolConfigName();
            // if no pool name has been explicitly set, then inject the *optional* "default mdb pool config"
            // If the default mdb pool config itself is not configured, then pooling is disabled for the bean
            if (poolName == null) {
                if (mdbComponentDescription.isDefaultMdbPoolAvailable()) {
                    serviceBuilder.addDependency(StrictMaxPoolConfigService.DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME,
                            PoolConfig.class, mdbComponentCreateService.getPoolConfigInjector());
                }
            } else {
                // pool name has been explicitly set so the pool config is a required dependency
                serviceBuilder.addDependency(StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName),
                        PoolConfig.class, mdbComponentCreateService.getPoolConfigInjector());
            }
        }
    }

    /**
     * A dependency configurator which adds a dependency/injection into the {@link MessageDrivenComponentCreateService}
     * for the appropriate resource adapter service
     */
    private class ResourceAdapterInjectingConfiguration implements DependencyConfigurator<MessageDrivenComponentCreateService> {

        @Override
        public void configureDependency(ServiceBuilder<?> serviceBuilder, MessageDrivenComponentCreateService service) throws DeploymentUnitProcessingException {
            final ServiceName raServiceName =
                ConnectorServices.getResourceAdapterServiceName(MessageDrivenComponentDescription.this.resourceAdapterName);
            // add the dependency on the RA service
            serviceBuilder.addDependency(raServiceName, ResourceAdapter.class, service.getResourceAdapterInjector());
        }
    }

    @Override
    public boolean isTimerServiceApplicable() {
        return true;
    }

    @Override
    public MessageDrivenBeanMetaData getDescriptorData() {
        return (MessageDrivenBeanMetaData) super.getDescriptorData();
    }

}
