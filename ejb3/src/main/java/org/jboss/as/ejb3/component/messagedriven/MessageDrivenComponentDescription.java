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


import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import javax.ejb.MessageDrivenBean;
import javax.ejb.TransactionManagementType;
import javax.resource.spi.ResourceAdapter;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PoolConfigService;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptor;
import org.jboss.as.ejb3.tx.EjbBMTInterceptor;
import org.jboss.as.ejb3.tx.TimerCMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentDescription extends EJBComponentDescription {
    private final Properties activationProps;
    private String resourceAdapterName;

    private String mdbPoolConfigName;

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
                                             final String defaultResourceAdapterName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
        if (messageListenerInterfaceName == null || messageListenerInterfaceName.isEmpty()) {
            throw EjbMessages.MESSAGES.stringParamCannotBeNullOrEmpty("Message listener interface");
        }
        if (defaultResourceAdapterName == null || defaultResourceAdapterName.trim().isEmpty()) {
            throw EjbMessages.MESSAGES.stringParamCannotBeNullOrEmpty("Default resource adapter name");
        }
        this.resourceAdapterName = defaultResourceAdapterName;
        this.activationProps = activationProps;

        registerView(messageListenerInterfaceName, MethodIntf.MESSAGE_ENDPOINT);
        // add the interceptor which will invoke the setMessageDrivenContext() method on a MDB which implements
        // MessageDrivenBean interface
        this.addSetMessageDrivenContextMethodInvocationInterceptor();
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassIndex classIndex, final ClassLoader moduleClassLoder) {
        final ComponentConfiguration mdbComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoder);
        // setup the component create service
        mdbComponentConfiguration.setComponentCreateServiceFactory(new MessageDrivenComponentCreateServiceFactory());

        // setup the configurator to inject the PoolConfig in the MessageDrivenComponentCreateService
        final MessageDrivenComponentDescription mdbComponentDescription = (MessageDrivenComponentDescription) mdbComponentConfiguration.getComponentDescription();
        mdbComponentConfiguration.getCreateDependencies().add(new PoolInjectingConfigurator(mdbComponentDescription));

        // setup the configurator to inject the resource adapter
        mdbComponentConfiguration.getCreateDependencies().add(new ResourceAdapterInjectingConfiguration());

        // add the bmt interceptor
        if (TransactionManagementType.BEAN.equals(this.getTransactionManagementType())) {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                    configuration.addTimeoutInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR);
                }
            });
        } else {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    configuration.addTimeoutInterceptor(TimerCMTTxInterceptor.FACTORY, InterceptorOrder.Component.COMPONENT_CMT_INTERCEPTOR);
                }
            });
        }


        return mdbComponentConfiguration;
    }


    public Properties getActivationProps() {
        return activationProps;
    }

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        if (resourceAdapterName == null || resourceAdapterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource adapter name cannot be null or empty");
        }
        this.resourceAdapterName = resourceAdapterName;
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let the super do its job
        super.setupViewInterceptors(view);

        // add the instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(MessageDrivenComponentInstanceAssociatingFactory.instance(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });

        //add the transaction interceptor
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
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
     * Adds a interceptor to invoke the {@link MessageDrivenBean#setMessageDrivenContext(javax.ejb.MessageDrivenContext)}
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

    private class PoolInjectingConfigurator implements DependencyConfigurator<Service<Component>> {

        private final MessageDrivenComponentDescription mdbComponentDescription;

        PoolInjectingConfigurator(final MessageDrivenComponentDescription mdbComponentDescription) {
            this.mdbComponentDescription = mdbComponentDescription;
        }

        @Override
        public void configureDependency(ServiceBuilder<?> serviceBuilder, Service<Component> service) throws DeploymentUnitProcessingException {
            final MessageDrivenComponentCreateService mdbComponentCreateService = (MessageDrivenComponentCreateService) service;
            final String poolName = this.mdbComponentDescription.getPoolConfigName();
            // if no pool name has been explicitly set, then inject the optional "default mdb pool config"
            if (poolName == null) {
                serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, PoolConfigService.DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME,
                        PoolConfig.class, mdbComponentCreateService.getPoolConfigInjector());
            } else {
                // pool name has been explicitly set so the pool config is a required dependency
                serviceBuilder.addDependency(PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName),
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
            final String suffixStrippedRaName = this.stripDotRarSuffix(MessageDrivenComponentDescription.this.resourceAdapterName);
            final Set<ServiceName> raServiceNames = ConnectorServices.getResourceAdapterServiceNames(suffixStrippedRaName);
            if (raServiceNames == null || raServiceNames.isEmpty()) {
                throw MESSAGES.failToFindResourceAdapter(suffixStrippedRaName);
            }
            final ServiceName raServiceName = raServiceNames.iterator().next();
            // add the dependency on the RA service
            serviceBuilder.addDependency(raServiceName, ResourceAdapter.class, service.getResourceAdapterInjector());
        }

        private String stripDotRarSuffix(final String raName) {
            if (raName == null) {
                return null;
            }
            // See RaDeploymentParsingProcessor
            if (raName.endsWith(".rar")) {
                return raName.substring(0, raName.indexOf(".rar"));
            }
            return raName;
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
