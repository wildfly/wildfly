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
import org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor;
import org.jboss.as.ejb3.component.session.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;

/**
 * User: jpai
 */
public class StatelessComponentDescription extends SessionBeanComponentDescription {

    /**
     * Construct a new instance.
     *
     * @param componentName        the component name
     * @param componentClassName   the component instance class name
     * @param ejbModuleDescription the module description
     */
    public StatelessComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbModuleDescription,
                                         final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbModuleDescription, deploymentUnitServiceName);
    }

    @Override
    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {
        final ComponentConfiguration statelessComponentConfiguration = new ComponentConfiguration(this, applicationDescription.getClassConfiguration(getComponentClassName()));
        // setup the component create service
        statelessComponentConfiguration.setComponentCreateServiceFactory(new StatelessComponentCreateServiceFactory());

        //setup the BMT interceptor

        // add the bmt interceptor
        if (TransactionManagementType.BEAN.equals(this.getTransactionManagementType())) {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                   final ComponentInstanceInterceptorFactory slsbBmtInterceptorFactory = new ComponentInstanceInterceptorFactory() {
                        @Override
                        protected Interceptor create(Component component, InterceptorFactoryContext context) {
                            if (component instanceof StatelessSessionComponent == false) {
                                throw new IllegalArgumentException("Component " + component + " with component class: " + component.getComponentClass() +
                                        " isn't a stateless component");
                            }
                            return new StatelessBMTInterceptor((StatelessSessionComponent) component);
                        }
                    };
                    // add the bmt interceptor factory
                    configuration.addComponentInterceptor(slsbBmtInterceptorFactory, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                }
            });
        }


        return statelessComponentConfiguration;
    }

    @Override
    public boolean allowsConcurrentAccess() {
        return false;
    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.STATELESS;
    }

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let super do its job first
        super.setupViewInterceptors(view);

        // add the instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                //add equals/hashCode interceptor
                //add equals/hashCode interceptor
                for(Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if((method.getName().equals("hashCode") && method.getParameterTypes().length==0) ||
                            method.getName().equals("equals") && method.getParameterTypes().length ==1 &&
                                    method.getParameterTypes()[0] == Object.class) {
                        configuration.addViewInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.View.SESSION_BEAN_EQUALS_HASHCODE);
                    }
                }

                // add the stateless component instance associating interceptor
                configuration.addViewInterceptor(PooledInstanceInterceptor.pooled(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
    }
}
