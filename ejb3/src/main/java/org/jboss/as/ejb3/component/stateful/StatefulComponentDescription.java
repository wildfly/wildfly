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


import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.StatefulBMTInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * User: jpai
 */
public class StatefulComponentDescription extends SessionBeanComponentDescription {

    private Method afterBegin;
    private Method afterCompletion;
    private Method beforeCompletion;
    private final Set<Method> prePassivateMethods = new HashSet<Method>();
    private final Set<Method> postActivateMethods = new HashSet<Method>();
    private final Map<MethodIdentifier, StatefulRemoveMethod> removeMethods = new HashMap<MethodIdentifier, StatefulRemoveMethod>();
    private StatefulTimeoutInfo statefulTimeout;
    private CacheInfo cache;

    /**
     * Map of init method, to the corresponding home create method on the home interface
     */
    private Map<Method, String> initMethods = new HashMap<Method, String>(0);

    public class StatefulRemoveMethod {
        private final MethodIdentifier methodIdentifier;
        private final boolean retainIfException;

        StatefulRemoveMethod(final MethodIdentifier method, final boolean retainIfException) {
            if (method == null) {
                throw MESSAGES.removeMethodIsNull();
            }
            this.methodIdentifier = method;
            this.retainIfException = retainIfException;
        }

        public MethodIdentifier getMethodIdentifier() {
            return methodIdentifier;
        }

        public boolean isRetainIfException() {
            return retainIfException;
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

        addInitMethodInvokingInterceptor();
    }

    private void addInitMethodInvokingInterceptor() {
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(StatefulInitMethodInterceptorFactory.INSTANCE, InterceptorOrder.ComponentPostConstruct.SFSB_INIT_METHOD);
            }
        });
    }

    private void addStatefulSessionSynchronizationInterceptor() {
        // we must run before the DefaultFirstConfigurator
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final InterceptorFactory interceptorFactory = StatefulSessionSynchronizationInterceptor.factory(getTransactionManagementType());
                configuration.addComponentInterceptor(interceptorFactory, InterceptorOrder.Component.SYNCHRONIZATION_INTERCEPTOR, false);
            }
        });

    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassIndex classIndex, final ClassLoader moduleClassLoder) {

        final ComponentConfiguration statefulComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoder);
        // setup the component create service
        statefulComponentConfiguration.setComponentCreateServiceFactory(new StatefulComponentCreateServiceFactory());

        if (getTransactionManagementType() == TransactionManagementType.BEAN) {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final ComponentInstanceInterceptorFactory bmtComponentInterceptorFactory = new ComponentInstanceInterceptorFactory() {
                        @Override
                        protected Interceptor create(Component component, InterceptorFactoryContext context) {
                            if (!(component instanceof StatefulSessionComponent)) {
                                throw MESSAGES.componentNotInstanceOfSessionComponent(component, component.getComponentClass(), "stateful");
                            }
                            return new StatefulBMTInterceptor((StatefulSessionComponent) component);
                        }
                    };
                    configuration.addComponentInterceptor(bmtComponentInterceptorFactory, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                }
            });
        }
        addStatefulSessionSynchronizationInterceptor();

        return statefulComponentConfiguration;
    }

    @Override
    public boolean allowsConcurrentAccess() {
        return true;
    }

    public Method getAfterBegin() {
        return afterBegin;
    }

    public Method getAfterCompletion() {
        return afterCompletion;
    }

    public Method getBeforeCompletion() {
        return beforeCompletion;
    }

    public Set<Method> getPrePassivateMethods() {
        return Collections.unmodifiableSet(this.prePassivateMethods);
    }

    public Set<Method> getPostActivateMethods() {
        return Collections.unmodifiableSet(this.postActivateMethods);
    }

    @Override
    public SessionBeanType getSessionBeanType() {
        return SessionBeanComponentDescription.SessionBeanType.STATEFUL;
    }

    public void setAfterBegin(final Method afterBegin) {
        this.afterBegin = afterBegin;
    }

    public void setAfterCompletion(final Method afterCompletion) {
        this.afterCompletion = afterCompletion;
    }

    public void setBeforeCompletion(final Method afterCompletion) {
        this.beforeCompletion = afterCompletion;
    }

    public void addPrePassivateMethod(final Method prePassivate) {
        this.prePassivateMethods.add(prePassivate);
    }

    public void addPostActivateMethod(final Method postActivate) {
        this.postActivateMethods.add(postActivate);
    }

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let super do its job
        super.setupViewInterceptors(view);
        // add the @Remove method interceptor
        this.addRemoveMethodInterceptor(view);
        // setup the instance associating interceptors
        this.addStatefulInstanceAssociatingInterceptor(view);

        this.addViewSerializationInterceptor(view);

        if (view instanceof EJBViewDescription) {
            EJBViewDescription ejbViewDescription = (EJBViewDescription) view;
            if (ejbViewDescription.getMethodIntf() == MethodIntf.REMOTE) {
                view.getConfigurators().add(new ViewConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                        final String earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
                        configuration.setViewInstanceFactory(new StatefulRemoteViewInstanceFactory(earApplicationName, componentConfiguration.getModuleName(), componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName(), componentConfiguration.getComponentName()));
                    }
                });
            }
        }
    }

    @Override
    protected ViewConfigurator getSessionBeanObjectViewConfigurator() {
        return StatefulSessionBeanObjectViewConfigurator.INSTANCE;
    }

    private void addViewSerializationInterceptor(final ViewDescription view) {
        view.setSerializable(true);
        view.setUseWriteReplace(true);
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final DeploymentReflectionIndex index = context.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
                ClassReflectionIndex<WriteReplaceInterface> classIndex = index.getClassIndex(WriteReplaceInterface.class);
                for (Method method : classIndex.getMethods()) {
                    configuration.addClientInterceptor(method, new WriteReplaceInterceptor.Factory(configuration.getViewServiceName().getCanonicalName()), InterceptorOrder.Client.WRITE_REPLACE);
                }
            }
        });
    }

    public void addRemoveMethod(final MethodIdentifier removeMethod, final boolean retainIfException) {
        if (removeMethod == null) {
            throw MESSAGES.removeMethodIsNull();
        }
        this.removeMethods.put(removeMethod, new StatefulRemoveMethod(removeMethod, retainIfException));
    }

    public Collection<StatefulRemoveMethod> getRemoveMethods() {
        return this.removeMethods.values();
    }

    public StatefulTimeoutInfo getStatefulTimeout() {
        return statefulTimeout;
    }

    public void setStatefulTimeout(final StatefulTimeoutInfo statefulTimeout) {
        this.statefulTimeout = statefulTimeout;
    }

    private void addStatefulInstanceAssociatingInterceptor(final EJBViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
                EJBViewDescription ejbViewDescription = (EJBViewDescription) view;
                //if this is a home interface we add a different interceptor
                if (ejbViewDescription.getMethodIntf() == MethodIntf.HOME || ejbViewDescription.getMethodIntf() == MethodIntf.LOCAL_HOME) {
                    for (Method method : viewConfiguration.getProxyFactory().getCachedMethods()) {
                        if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                                method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                        method.getParameterTypes()[0] == Object.class) {
                            viewConfiguration.addClientInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
                        }
                    }
                } else {
                    // interceptor factory return an interceptor which sets up the session id on component view instance creation
                    final InterceptorFactory sessionIdGeneratingInterceptorFactory = StatefulComponentSessionIdGeneratingInterceptorFactory.INSTANCE;

                    // add the session id generating interceptor to the start of the *post-construct interceptor chain of the ComponentViewInstance*
                    viewConfiguration.addClientPostConstructInterceptor(sessionIdGeneratingInterceptorFactory, InterceptorOrder.ClientPostConstruct.INSTANCE_CREATE);

                    for (Method method : viewConfiguration.getProxyFactory().getCachedMethods()) {
                        if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                                method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                        method.getParameterTypes()[0] == Object.class) {
                            viewConfiguration.addClientInterceptor(method, StatefulIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
                        }
                    }
                }
            }
        });
        if (view.getMethodIntf() != MethodIntf.LOCAL_HOME && view.getMethodIntf() != MethodIntf.HOME) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    // add the instance associating interceptor to the *start of the invocation interceptor chain*
                    configuration.addClientInterceptor(StatefulComponentIdInterceptor.Factory.INSTANCE, InterceptorOrder.Client.ASSOCIATING_INTERCEPTOR);
                    configuration.addViewInterceptor(StatefulComponentInstanceInterceptor.Factory.INSTANCE, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
                }
            });
        }

    }

    private void addRemoveMethodInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final StatefulComponentDescription statefulComponentDescription = (StatefulComponentDescription) componentConfiguration.getComponentDescription();
                final Collection<StatefulRemoveMethod> removeMethods = statefulComponentDescription.getRemoveMethods();
                if (removeMethods.isEmpty()) {
                    return;
                }
                for (final Method viewMethod : configuration.getProxyFactory().getCachedMethods()) {
                    final MethodIdentifier viewMethodIdentifier = MethodIdentifier.getIdentifierForMethod(viewMethod);
                    for (final StatefulRemoveMethod removeMethod : removeMethods) {
                        if (removeMethod.methodIdentifier.equals(viewMethodIdentifier)) {

                            //we do not want to add this if it is the Ejb(Local)Object.remove() method, as that is handed elsewhere
                            final boolean object = EJBObject.class.isAssignableFrom(configuration.getViewClass()) || EJBLocalObject.class.isAssignableFrom(configuration.getViewClass());
                            if (!object || !viewMethodIdentifier.getName().equals("remove") || viewMethodIdentifier.getParameterTypes().length != 0) {
                                configuration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(new StatefulRemoveInterceptor(removeMethod.retainIfException)), InterceptorOrder.View.SESSION_REMOVE_INTERCEPTOR);
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    public void addInitMethod(final Method method, final String createMethod) {
        initMethods.put(method, createMethod);
    }

    public Map<Method, String> getInitMethods() {
        return Collections.unmodifiableMap(initMethods);
    }

    public CacheInfo getCache() {
        return this.cache;
    }

    public void setCache(CacheInfo cache) {
        this.cache = cache;
    }
}
