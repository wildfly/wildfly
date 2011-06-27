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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.concurrency.ContainerManagedConcurrencyInterceptorFactory;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.SingletonLifecycleCMTTxInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceName;

import javax.ejb.ConcurrencyManagementType;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
                                         final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
        // add container managed concurrency interceptor to the component
        this.addConcurrencyManagementInterceptor();

    }

    @Override
    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {

        ComponentConfiguration singletonComponentConfiguration = new ComponentConfiguration(this, applicationDescription.getClassConfiguration(getComponentClassName()));
        // setup the component create service
        singletonComponentConfiguration.setComponentCreateServiceFactory(new SingletonComponentCreateServiceFactory(this.isInitOnStartup(), dependsOn));

        if(getTransactionManagementType().equals(TransactionManagementType.CONTAINER)) {
            //we need to add the transaction interceptor to the lifecycle methods
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                    if(getClassDescription().getPostConstructMethod() != null) {
                        TransactionAttributeType txAttr = getTransactionAttribute(MethodIntf.BEAN, getClassDescription().getClassName(), getClassDescription().getPostConstructMethod().getName(), getClassDescription().getPostConstructMethod().getParameterTypes());
                        configuration.addPostConstructInterceptor(new SingletonLifecycleCMTTxInterceptorFactory(txAttr), InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    }
                    if(getClassDescription().getPreDestroyMethod() != null) {
                        TransactionAttributeType txAttr = getTransactionAttribute(MethodIntf.BEAN, getClassDescription().getClassName(), getClassDescription().getPreDestroyMethod().getName(), getClassDescription().getPreDestroyMethod().getParameterTypes());
                        configuration.addPreDestroyInterceptor(new SingletonLifecycleCMTTxInterceptorFactory(txAttr), InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);
                    }
                }
            });
        } else {
            // add the bmt interceptor
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final ComponentInstanceInterceptorFactory slsbBmtInterceptorFactory = new ComponentInstanceInterceptorFactory() {
                        @Override
                        protected Interceptor create(Component component, InterceptorFactoryContext context) {
                            if (component instanceof SingletonComponent == false) {
                                throw new IllegalArgumentException("Component " + component + " with component class: " + component.getComponentClass() +
                                        " isn't a singleton component");
                            }
                            return new SingletonBMTInterceptor((SingletonComponent) component);
                        }
                    };
                    if(getClassDescription().getPostConstructMethod() != null) {
                        configuration.addPostConstructInterceptor(slsbBmtInterceptorFactory, InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    }
                    if(getClassDescription().getPreDestroyMethod() != null) {
                        configuration.addPreDestroyInterceptor(slsbBmtInterceptorFactory, InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);
                    }
                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(slsbBmtInterceptorFactory, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
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
    public boolean allowsConcurrentAccess() {
        return true;
    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.SINGLETON;
    }

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);

        // add instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                //add equals/hashCode interceptor
                for (Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                            method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                    method.getParameterTypes()[0] == Object.class) {
                        configuration.addViewInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.View.SESSION_BEAN_EQUALS_HASHCODE);
                    }
                }

                // add the singleton component instance associating interceptor
                configuration.addViewInterceptor(SingletonComponentInstanceAssociationInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
    }

    private void addConcurrencyManagementInterceptor() {
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final SingletonComponentDescription singletonComponentDescription = (SingletonComponentDescription) description;
                // we don't care about BEAN managed concurrency, so just return
                if (singletonComponentDescription.getConcurrencyManagementType() == ConcurrencyManagementType.BEAN) {
                    return;
                }
                configuration.addComponentInterceptor(ContainerManagedConcurrencyInterceptorFactory.INSTANCE, InterceptorOrder.Component.SINGLETON_CONTAINER_MANAGED_CONCURRENCY_INTERCEPTOR, true);
            }
        });
    }

    public List<ServiceName> getDependsOn() {
        return dependsOn;
    }
}
