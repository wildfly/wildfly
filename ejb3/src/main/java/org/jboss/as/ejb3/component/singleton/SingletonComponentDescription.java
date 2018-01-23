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

package org.jboss.as.ejb3.component.singleton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.ejb.ConcurrencyManagementType;
import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.session.StatelessRemoteViewInstanceFactory;
import org.jboss.as.ejb3.component.session.StatelessWriteReplaceInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.security.SecurityContextInterceptorFactory;
import org.jboss.as.ejb3.tx.EjbBMTInterceptor;
import org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor;
import org.jboss.as.ejb3.tx.TimerCMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * Component description for a singleton bean
 *
 * @author Jaikiran Pai
 */
public class SingletonComponentDescription extends SessionBeanComponentDescription {

    /**
     * Flag to indicate whether the singleton bean is a @Startup (a.k.a init-on-startup) bean
     */
    private boolean initOnStartup;

    private final List<ServiceName> dependsOn = new ArrayList<ServiceName>();

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module description
     */
    public SingletonComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription,
                                         final ServiceName deploymentUnitServiceName, final SessionBeanMetaData descriptorData) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName, descriptorData);

        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addTimeoutViewInterceptor(SingletonComponentInstanceAssociationInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
                ConcurrencyManagementType concurrencyManagementType = getConcurrencyManagementType();
                if (concurrencyManagementType == null || concurrencyManagementType == ConcurrencyManagementType.CONTAINER) {
                    configuration.addTimeoutViewInterceptor(new ContainerManagedConcurrencyInterceptorFactory(Collections.emptyMap()), InterceptorOrder.View.SINGLETON_CONTAINER_MANAGED_CONCURRENCY_INTERCEPTOR);
                }

            }
        });
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {

        ComponentConfiguration singletonComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
        // setup the component create service
        singletonComponentConfiguration.setComponentCreateServiceFactory(new SingletonComponentCreateServiceFactory(this.isInitOnStartup(), dependsOn));
        if(isExplicitSecurityDomainConfigured()) {
            getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
                        String contextID = deploymentUnit.getName();
                        if (deploymentUnit.getParent() != null) {
                            contextID = deploymentUnit.getParent().getName() + "!" + contextID;
                        }
                        EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;
                        final boolean securityRequired = isExplicitSecurityDomainConfigured();
                        ejbComponentDescription.setSecurityRequired(securityRequired);
                        if (isSecurityDomainKnown()) {
                            final HashMap<Integer, InterceptorFactory> elytronInterceptorFactories = getElytronInterceptorFactories(contextID, ejbComponentDescription.isEnableJacc(), false);
                            elytronInterceptorFactories.forEach((priority, elytronInterceptorFactory) -> configuration.addPostConstructInterceptor(elytronInterceptorFactory, priority));
                        } else {
                            configuration.addPostConstructInterceptor(new SecurityContextInterceptorFactory(securityRequired, false, contextID), InterceptorOrder.View.SECURITY_CONTEXT);
                        }
                    }
                });
        }
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                if (isInitOnStartup()) {
                    final StartupCountdown startupCountdown = context.getDeploymentUnit().getAttachment(Attachments.STARTUP_COUNTDOWN);
                    configuration.addPostConstructInterceptor(new ImmediateInterceptorFactory(new StartupCountDownInterceptor(startupCountdown)), InterceptorOrder.ComponentPostConstruct.STARTUP_COUNTDOWN_INTERCEPTOR);
                }
            }
        });
        if (getTransactionManagementType().equals(TransactionManagementType.CONTAINER)) {
            //we need to add the transaction interceptor to the lifecycle methods
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    final EEApplicationClasses applicationClasses = context.getDeploymentUnit().getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
                    InterceptorClassDescription interceptorConfig = ComponentDescription.mergeInterceptorConfig(configuration.getComponentClass(), applicationClasses.getClassByName(description.getComponentClassName()), description, MetadataCompleteMarker.isMetadataComplete(context.getDeploymentUnit()));

                    if(interceptorConfig.getPostConstruct() != null) {
                        configuration.addPostConstructInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPostConstruct(), true), InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    }
                    configuration.addPreDestroyInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPreDestroy() ,true), InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);

                    configuration.addTimeoutViewInterceptor(TimerCMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);

                }
            });
        } else {
            // add the bmt interceptor
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    configuration.addPostConstructInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    configuration.addPreDestroyInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);
                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(EjbBMTInterceptor.FACTORY, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);

                }
            });
        }

        return singletonComponentConfiguration;
    }

    /**
     * Returns true if the singleton bean is marked for init-on-startup (a.k.a @Startup). Else
     * returns false
     * <p/>
     *
     * @return
     */
    public boolean isInitOnStartup() {
        return this.initOnStartup;
    }

    /**
     * Marks the singleton bean for init-on-startup
     */
    public void initOnStartup() {
        this.initOnStartup = true;

    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.SINGLETON;
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);
        addViewSerializationInterceptor(view);

        // add container managed concurrency interceptor to the component
        this.addConcurrencyManagementInterceptor(view);

        // add instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                //add equals/hashCode interceptor
                for (Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                            method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                    method.getParameterTypes()[0] == Object.class) {
                        configuration.addClientInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
                    }
                }

                // add the singleton component instance associating interceptor
                configuration.addViewInterceptor(SingletonComponentInstanceAssociationInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });


        if (view.getMethodIntf() == MethodIntf.REMOTE) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    final String earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
                    configuration.setViewInstanceFactory(new StatelessRemoteViewInstanceFactory(earApplicationName, componentConfiguration.getModuleName(), componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName(), componentConfiguration.getComponentName()));
                }
            });
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
                for (Method method : classIndex.getMethods()) {
                    configuration.addClientInterceptor(method, StatelessWriteReplaceInterceptor.factory(configuration.getViewServiceName().getCanonicalName()), InterceptorOrder.Client.WRITE_REPLACE);
                }
            }
        });
    }

    @Override
    protected ViewConfigurator getSessionBeanObjectViewConfigurator() {
        throw EjbLogger.ROOT_LOGGER.ejb2xViewNotApplicableForSingletonBeans();
    }

    private void addConcurrencyManagementInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                final SingletonComponentDescription singletonComponentDescription = (SingletonComponentDescription) componentConfiguration.getComponentDescription();
                // we don't care about BEAN managed concurrency, so just return
                if (singletonComponentDescription.getConcurrencyManagementType() == ConcurrencyManagementType.BEAN) {
                    return;
                }
                configuration.addViewInterceptor(new ContainerManagedConcurrencyInterceptorFactory(configuration.getViewToComponentMethodMap()), InterceptorOrder.View.SINGLETON_CONTAINER_MANAGED_CONCURRENCY_INTERCEPTOR);
            }
        });
    }

    public List<ServiceName> getDependsOn() {
        return dependsOn;
    }

    @Override
    public boolean isTimerServiceApplicable() {
        return true;
    }

}
