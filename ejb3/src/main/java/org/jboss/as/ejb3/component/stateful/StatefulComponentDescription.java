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

package org.jboss.as.ejb3.component.stateful;


import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionManagementType;

/**
 * User: jpai
 */
public class StatefulComponentDescription extends SessionBeanComponentDescription {

    private static final Logger logger = Logger.getLogger(StatefulComponentDescription.class);

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module description
     */
    public StatefulComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription,
                                        final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
    }

    @Override
    public ComponentConfiguration createConfiguration(EEModuleConfiguration moduleConfiguration) {

        final ComponentConfiguration statefulComponentConfiguration = new ComponentConfiguration(this, moduleConfiguration.getClassConfiguration(getComponentClassName()));
        // setup the component create service
        statefulComponentConfiguration.setComponentCreateServiceFactory(new StatefulComponentCreateServiceFactory());
        return statefulComponentConfiguration;
    }

    @Override
    public boolean allowsConcurrentAccess() {
        return true;
    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.STATEFUL;
    }

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let super do its job
        super.setupViewInterceptors(view);

        final Object sessionIdContextKey = new Object();
        // add the session id generating interceptor to the start of the *post-construct interceptor chain of the ComponentViewInstance*
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
                // interceptor factory return an interceptor which sets up the session id on component view instance creation
                InterceptorFactory sessionIdGeneratingInterceptorFactory = new StatefulComponentSessionIdGeneratingInterceptorFactory(sessionIdContextKey);
                // add the session id generating interceptor to the start of the *post-construct interceptor chain of the ComponentViewInstance*
                viewConfiguration.getViewPostConstructInterceptors().addFirst(sessionIdGeneratingInterceptorFactory);
                viewConfiguration.getViewPreDestroyInterceptors().addFirst(new StatefulComponentInstanceDestroyInterceptorFactory(sessionIdContextKey));
            }
        });

        // add the instance associating interceptor to the *start of the invocation interceptor chain*
        view.getConfigurators().addFirst(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the stateful component instance associator
                configuration.addViewInterceptorToFront(new StatefulComponentInstanceInterceptorFactory(sessionIdContextKey));
            }
        });

        // for CMT, setup the session sychronization tx interceptor
        if (TransactionManagementType.CONTAINER.equals(this.getTransactionManagementType())) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    logger.warn("Interceptors at ComponentInstance level aren't supported yet - SessionSynchronization semantics for Stateful beans with CMT won't work!");
                }
            });
        } else { // setup BMT interceptor
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    final ComponentInterceptorFactory bmtComponentInterceptorFactory = new ComponentInterceptorFactory() {
                        @Override
                        protected Interceptor create(Component component, InterceptorFactoryContext context) {
                            if (component instanceof StatefulSessionComponent == false) {
                                throw new IllegalArgumentException("Component " + component + " with component class: " + component.getComponentClass() +
                                        " isn't a stateful component");
                            }
                            return new StatefulBMTInterceptor((StatefulSessionComponent) component);
                        }
                    };
                    // add the bmt interceptor factory to the view
                    configuration.addViewInterceptorToFront(bmtComponentInterceptorFactory);
                }
            });
        }

    }
}
