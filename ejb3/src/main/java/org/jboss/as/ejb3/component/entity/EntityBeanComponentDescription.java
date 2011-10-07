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

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EjbHomeViewDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanReentrancyInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanSynchronizationInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityInvocationContextInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.ejb.spec.PersistenceType;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionManagementType;

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
            }
        });
    }

    @Override
    protected void addCurrentInvocationContextFactory() {

    }

    @Override
    protected void addCurrentInvocationContextFactory(ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(EntityInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
            }
        });

    }


    @Override
    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {

        final ComponentConfiguration configuration = new ComponentConfiguration(this, applicationDescription.getClassConfiguration(getComponentClassName()));
        // setup the component create service
        configuration.setComponentCreateServiceFactory(EntityBeanComponentCreateService.FACTORY);

        return configuration;
    }

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);

        // add a Tx configurator
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
                // Add CMT interceptor factory
                if (TransactionManagementType.CONTAINER.equals(ejbComponentDescription.getTransactionManagementType())) {
                    configuration.addViewInterceptor(CMTTxInterceptorFactory.INSTANCE, InterceptorOrder.View.CMT_TRANSACTION_INTERCEPTOR);
                }
            }
        });

        //now we need to figure out if this is a home or object view
        if (view instanceof EjbHomeViewDescription) {
            view.getConfigurators().add(new EntityBeanHomeViewConfigurator());
        } else {
            view.getConfigurators().add(new EntityBeanObjectViewConfigurator());
        }

    }


    private void addSynchronizationInterceptor() {
        // we must run before the DefaultFirstConfigurator
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addComponentInterceptor(EntityBeanSynchronizationInterceptor.FACTORY, InterceptorOrder.Component.SYNCHRONIZATION_INTERCEPTOR, false);
                if (!reentrant) {
                    configuration.addComponentInterceptor(EntityBeanReentrancyInterceptor.FACTORY, InterceptorOrder.Component.REENTRANCY_INTERCEPTOR, false);
                }
            }
        });

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


}
