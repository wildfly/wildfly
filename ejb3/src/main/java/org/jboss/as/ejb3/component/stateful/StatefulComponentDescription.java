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
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: jpai
 */
public class StatefulComponentDescription extends SessionBeanComponentDescription {

    private static final Logger logger = Logger.getLogger(StatefulComponentDescription.class);

    private Set<StatefulRemoveMethod> removeMethods = new HashSet<StatefulRemoveMethod>();

    private class StatefulRemoveMethod {
        private final MethodIdentifier methodIdentifier;
        private final boolean retainIfException;

        StatefulRemoveMethod(final MethodIdentifier method, final boolean retainIfException) {
            if (method == null) {
                throw new IllegalArgumentException("@Remove method cannot be null");
            }
            this.methodIdentifier = method;
            this.retainIfException = retainIfException;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StatefulRemoveMethod that = (StatefulRemoveMethod) o;

            if (!methodIdentifier.equals(that.methodIdentifier)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return methodIdentifier.hashCode();
        }
    }

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

        addStatefulSessionSynchronizationInterceptor();
    }

    private void addStatefulSessionSynchronizationInterceptor() {
        // we must run before the DefaultFirstConfigurator
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final InterceptorFactory interceptorFactory = new ComponentInstanceInterceptorFactory() {
                    @Override
                    protected Interceptor create(ComponentInstance instance, InterceptorFactoryContext context) {
                        return new StatefulSessionSynchronizationInterceptor();
                    }
                };
                configuration.addComponentInterceptor(interceptorFactory, InterceptorOrder.Component.SFSB_SYNCHRONIZATION_INTERCEPTOR, false);
            }
        });
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
        // add the @Remove method interceptor
        this.addRemoveMethodInterceptor(view);
        // setup the instance associating interceptors
        this.addStatefulInstanceAssociatingInterceptor(view);
        // setup tx management interceptors
        this.addTransactionManagementInterceptor(view);


    }

    public void addRemoveMethod(final MethodIdentifier removeMethod, final boolean retainIfException) {
        if (removeMethod == null) {
            throw new IllegalArgumentException("@Remove method identifier cannot be null");
        }
        this.removeMethods.add(new StatefulRemoveMethod(removeMethod, retainIfException));
    }

    public Set<StatefulRemoveMethod> getRemoveMethods() {
        return Collections.unmodifiableSet(this.removeMethods);
    }

    private void addStatefulInstanceAssociatingInterceptor(final ViewDescription view) {
        final Object sessionIdContextKey = new Object();
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
                // interceptor factory return an interceptor which sets up the session id on component view instance creation
                InterceptorFactory sessionIdGeneratingInterceptorFactory = new StatefulComponentSessionIdGeneratingInterceptorFactory(sessionIdContextKey);

                // add the session id generating interceptor to the start of the *post-construct interceptor chain of the ComponentViewInstance*
                viewConfiguration.addViewPostConstructInterceptor(sessionIdGeneratingInterceptorFactory, InterceptorOrder.ViewPostConstruct.INSTANCE_CREATE);
                viewConfiguration.addViewPreDestroyInterceptor(new StatefulComponentInstanceDestroyInterceptorFactory(sessionIdContextKey), InterceptorOrder.ViewPreDestroy.INSTANCE_DESTROY);

                for(Method method : viewConfiguration.getProxyFactory().getCachedMethods()) {
                    if((method.getName().equals("hashCode") && method.getParameterTypes().length==0) ||
                            method.getName().equals("equals") && method.getParameterTypes().length ==1 &&
                                    method.getParameterTypes()[0] == Object.class) {
                        viewConfiguration.addViewInterceptor(new StatefulIdentityInterceptorFactory(sessionIdContextKey), InterceptorOrder.View.SESSION_BEAN_EQUALS_HASHCODE);
                    }
                }
            }
        });

        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the instance associating interceptor to the *start of the invocation interceptor chain*
                configuration.addViewInterceptor(new StatefulComponentInstanceInterceptorFactory(sessionIdContextKey), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });

    }

    private void addRemoveMethodInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final StatefulComponentDescription statefulComponentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
                final Set<StatefulRemoveMethod> removeMethods = statefulComponentDescription.getRemoveMethods();
                if (removeMethods.isEmpty()) {
                    return;
                }
                for (final Method viewMethod : configuration.getProxyFactory().getCachedMethods()) {
                    final MethodIdentifier viewMethodIdentifier = MethodIdentifier.getIdentifierForMethod(viewMethod);
                    for (final StatefulRemoveMethod removeMethod : removeMethods) {
                        if (removeMethod.methodIdentifier.equals(viewMethodIdentifier)) {
                            configuration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(new StatefulRemoveInterceptor(removeMethod.retainIfException)), InterceptorOrder.View.SFSB_REMOVE_INTERCEPTOR);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void addTransactionManagementInterceptor(final ViewDescription view) {
        // for CMT, setup the session sychronization tx interceptor
        if (TransactionManagementType.CONTAINER.equals(this.getTransactionManagementType())) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                    logger.warn("Interceptors at ComponentInstance level aren't supported yet - SessionSynchronization semantics for " +
                            "Stateful beans with CMT won't work!");
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
                    configuration.addViewInterceptor(bmtComponentInterceptorFactory, InterceptorOrder.View.TRANSACTION_INTERCEPTOR);
                }
            });
        }
    }
}
