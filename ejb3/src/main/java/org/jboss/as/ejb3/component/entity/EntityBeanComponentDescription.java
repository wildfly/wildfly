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
package org.jboss.as.ejb3.component.entity;

import java.lang.reflect.Method;

import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.EjbHomeViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanAssociatingInterceptorFactory;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanReentrancyInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanRemoveInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanSynchronizationInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptor;
import org.jboss.as.ejb3.tx.TimerCMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.metadata.ejb.spec.PersistenceType;
import org.jboss.msc.service.ServiceName;

/**
 * Description of an old school entity bean.
 *
 * @author Stuart Douglas
 */
public class EntityBeanComponentDescription extends EJBComponentDescription {

    private PersistenceType persistenceType;
    private boolean reentrant;
    private String primaryKeyType;

    public EntityBeanComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
        addSynchronizationInterceptor();
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(EntityBeanInterceptors.POST_CONSTRUCT, InterceptorOrder.ComponentPostConstruct.SETUP_CONTEXT);
                configuration.addTimeoutViewInterceptor(EntityBeanAssociatingInterceptorFactory.INSTANCE, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
        addRemoveInterceptor();
    }

    protected void addRemoveInterceptor() {
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                for (Method method : configuration.getDefinedComponentMethods()) {
                    if (method.getName().equals("ejbRemove") && method.getParameterTypes().length == 0) {
                        configuration.addComponentInterceptor(method, EntityBeanRemoveInterceptor.FACTORY, InterceptorOrder.Component.ENTITY_BEAN_REMOVE_INTERCEPTOR);
                    }
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
                configuration.addPreDestroyInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPreDestroy.EJB_SESSION_CONTEXT_INTERCEPTOR);
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


    @Override
    public final ComponentConfiguration createConfiguration(final ClassIndex classIndex, final ClassLoader moduleClassLoder) {
        final ComponentConfiguration configuration = createEntityBeanConfiguration(classIndex, moduleClassLoder);
        // add the timer interceptor
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addTimeoutViewInterceptor(TimerCMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
            }
        });
        return configuration;
    }

    protected ComponentConfiguration createEntityBeanConfiguration(final ClassIndex classIndex, final ClassLoader moduleClassLoder) {
        final ComponentConfiguration configuration = new ComponentConfiguration(this, classIndex, moduleClassLoder);
        // setup the component create service
        configuration.setComponentCreateServiceFactory(EntityBeanComponentCreateService.FACTORY);
        return configuration;
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);

        // add a Tx configurator
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
                // Add CMT interceptor factory
                if (TransactionManagementType.CONTAINER.equals(ejbComponentDescription.getTransactionManagementType())) {
                    configuration.addViewInterceptor(CMTTxInterceptor.FACTORY, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
                }
            }
        });

        //now we need to figure out if this is a home or object view
        if (view instanceof EjbHomeViewDescription) {
            view.getConfigurators().add(getHomeViewConfigurator());
        } else {
            view.getConfigurators().add(getObjectViewConfigurator());
        }

        if (view.getMethodIntf() == MethodIntf.REMOTE) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    final EEModuleDescription moduleDescription = componentConfiguration.getComponentDescription().getModuleDescription();
                    final String appName = moduleDescription.getEarApplicationName() == null ? "" : moduleDescription.getEarApplicationName();
                    configuration.setViewInstanceFactory(getRemoteViewInstanceFactory(appName, componentConfiguration.getModuleName(), moduleDescription.getDistinctName(), componentConfiguration.getComponentName()));
                }
            });
        }

    }

    protected EntityBeanObjectViewConfigurator getObjectViewConfigurator() {
        return new EntityBeanObjectViewConfigurator();
    }

    protected EntityBeanHomeViewConfigurator getHomeViewConfigurator() {
        return new EntityBeanHomeViewConfigurator();
    }

    protected ViewInstanceFactory getRemoteViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String componentName) {
        return new EntityBeanRemoteViewInstanceFactory(applicationName, moduleName, distinctName, componentName);
    }

    protected void addSynchronizationInterceptor() {
        // we must run before the DefaultFirstConfigurator
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addComponentInterceptor(getSynchronizationInterceptorFactory(), InterceptorOrder.Component.SYNCHRONIZATION_INTERCEPTOR, false);
                if (!reentrant) {
                    configuration.addComponentInterceptor(EntityBeanReentrancyInterceptor.FACTORY, InterceptorOrder.Component.REENTRANCY_INTERCEPTOR, false);
                }
            }
        });
    }

    protected InterceptorFactory getSynchronizationInterceptorFactory() {
        return EntityBeanSynchronizationInterceptor.FACTORY;
    }

    public String getPrimaryKeyType() {
        return primaryKeyType;
    }

    public void setPrimaryKeyType(final String primaryKeyType) {
        this.primaryKeyType = primaryKeyType;
    }

    public boolean isReentrant() {
        return reentrant;
    }

    public void setReentrant(final boolean reentrant) {
        this.reentrant = reentrant;
    }

    public PersistenceType getPersistenceType() {
        return persistenceType;
    }

    public void setPersistenceType(final PersistenceType persistenceType) {
        this.persistenceType = persistenceType;
    }

    @Override
    public boolean isTimerServiceApplicable() {
        return true;
    }

}
