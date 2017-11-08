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
import java.util.Map;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.interceptors.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor;
import org.jboss.as.ejb3.tx.StatefulBMTInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * User: jpai
 */
public class StatefulComponentDescription extends SessionBeanComponentDescription {

    private Method afterBegin;
    private Method afterCompletion;
    private Method beforeCompletion;
    private final Map<MethodIdentifier, StatefulRemoveMethod> removeMethods = new HashMap<MethodIdentifier, StatefulRemoveMethod>();
    private StatefulTimeoutInfo statefulTimeout;
    private CacheInfo cache;
    // by default stateful beans are passivation capable, but beans can override it via annotation or deployment descriptor, starting EJB 3.2
    private boolean passivationApplicable = true;
    private final ServiceName deploymentUnitServiceName;

    /**
     * Map of init method, to the corresponding home create method on the home interface
     */
    private Map<Method, String> initMethods = new HashMap<Method, String>(0);

    public class StatefulRemoveMethod {
        private final MethodIdentifier methodIdentifier;
        private final boolean retainIfException;

        StatefulRemoveMethod(final MethodIdentifier method, final boolean retainIfException) {
            if (method == null) {
                throw EjbLogger.ROOT_LOGGER.removeMethodIsNull();
            }
            this.methodIdentifier = method;
            this.retainIfException = retainIfException;
        }

        public MethodIdentifier getMethodIdentifier() {
            return methodIdentifier;
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
                                        final ServiceName deploymentUnitServiceName, final SessionBeanMetaData descriptorData) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName, descriptorData);
        this.deploymentUnitServiceName = deploymentUnitServiceName;
        addInitMethodInvokingInterceptor();
    }

    private void addInitMethodInvokingInterceptor() {
        getConfigurators().addFirst(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(StatefulInitMethodInterceptor.INSTANCE, InterceptorOrder.ComponentPostConstruct.SFSB_INIT_METHOD);
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
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {

        final ComponentConfiguration statefulComponentConfiguration = new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
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
                                throw EjbLogger.ROOT_LOGGER.componentNotInstanceOfSessionComponent(component, component.getComponentClass(), "stateful");
                            }
                            return new StatefulBMTInterceptor((StatefulSessionComponent) component);
                        }
                    };
                    configuration.addComponentInterceptor(bmtComponentInterceptorFactory, InterceptorOrder.Component.BMT_TRANSACTION_INTERCEPTOR, false);
                }
            });
        } else {
            getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final EEApplicationClasses applicationClasses = context.getDeploymentUnit().getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
                    InterceptorClassDescription interceptorConfig = ComponentDescription.mergeInterceptorConfig(configuration.getComponentClass(), applicationClasses.getClassByName(description.getComponentClassName()), description, MetadataCompleteMarker.isMetadataComplete(context.getDeploymentUnit()));

                    configuration.addPostConstructInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPostConstruct(), false), InterceptorOrder.ComponentPostConstruct.TRANSACTION_INTERCEPTOR);
                    configuration.addPreDestroyInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPreDestroy(), false), InterceptorOrder.ComponentPreDestroy.TRANSACTION_INTERCEPTOR);

                    if (description.isPassivationApplicable()) {
                        configuration.addPrePassivateInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPrePassivate(), false), InterceptorOrder.ComponentPassivation.TRANSACTION_INTERCEPTOR);
                        configuration.addPostActivateInterceptor(new LifecycleCMTTxInterceptor.Factory(interceptorConfig.getPostActivate(), false), InterceptorOrder.ComponentPassivation.TRANSACTION_INTERCEPTOR);
                    }
                }
            });
        }
        addStatefulSessionSynchronizationInterceptor();

        return statefulComponentConfiguration;
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

    @Override
    protected void setupViewInterceptors(EJBViewDescription view) {
        // let super do its job
        super.setupViewInterceptors(view);
        // add the @Remove method interceptor
        this.addRemoveMethodInterceptor(view);
        // setup the instance associating interceptors
        this.addStatefulInstanceAssociatingInterceptor(view);

        this.addViewSerializationInterceptor(view);

        if (view.getMethodIntf() == MethodIntf.REMOTE) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    final String earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
                    configuration.setViewInstanceFactory(new StatefulRemoteViewInstanceFactory(earApplicationName, componentConfiguration.getModuleName(), componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName(), componentConfiguration.getComponentName()));
                }
            });
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
                ClassReflectionIndex classIndex = index.getClassIndex(WriteReplaceInterface.class);
                for (Method method : (Collection<Method>)classIndex.getMethods()) {
                    configuration.addClientInterceptor(method, new StatefulWriteReplaceInterceptor.Factory(configuration.getViewServiceName().getCanonicalName()), InterceptorOrder.Client.WRITE_REPLACE);
                }
            }
        });
    }

    public void addRemoveMethod(final MethodIdentifier removeMethod, final boolean retainIfException) {
        if (removeMethod == null) {
            throw EjbLogger.ROOT_LOGGER.removeMethodIsNull();
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
                    final InterceptorFactory sessionIdGeneratingInterceptorFactory = StatefulComponentSessionIdGeneratingInterceptor.FACTORY;

                    // add the session id generating interceptor to the start of the *post-construct interceptor chain of the ComponentViewInstance*
                    viewConfiguration.addClientPostConstructInterceptor(sessionIdGeneratingInterceptorFactory, InterceptorOrder.ClientPostConstruct.INSTANCE_CREATE);

                    for (Method method : viewConfiguration.getProxyFactory().getCachedMethods()) {
                        if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) ||
                                method.getName().equals("equals") && method.getParameterTypes().length == 1 &&
                                        method.getParameterTypes()[0] == Object.class) {
                            viewConfiguration.addClientInterceptor(method, StatefulIdentityInterceptor.FACTORY, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
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
                    configuration.addViewInterceptor(StatefulComponentInstanceInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
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

    @Override
    public boolean isPassivationApplicable() {
        return this.passivationApplicable;
    }

    public void setPassivationApplicable(final boolean passivationApplicable) {
        this.passivationApplicable = passivationApplicable;
    }

    /**
     * EJB 3.2 spec allows the TimeService to be injected/looked up/accessed from the stateful bean so as to allow access to the {@link javax.ejb.TimerService#getAllTimers()}
     * method from a stateful bean. Hence we make timerservice applicable for stateful beans too. However, we return <code>false</code> in {@link #isTimerServiceRequired()} so that a {@link org.jboss.as.ejb3.timerservice.NonFunctionalTimerService}
     * is made available for the stateful bean. The {@link org.jboss.as.ejb3.timerservice.NonFunctionalTimerService} only allows access to {@link javax.ejb.TimerService#getAllTimers()} and {@link javax.ejb.TimerService#getTimers()}
     * methods and throws an {@link IllegalStateException} for all othre methods on the timerservice and that's exactly how we want it to behave for stateful beans
     *
     * @return
     * @see {@link #isTimerServiceRequired()}
     */
    @Override
    public boolean isTimerServiceApplicable() {
        return true;
    }

    /**
     * Timeout methods and auto timer methods aren't applicable for stateful beans, hence we return false.
     * @return
     * @see {@link #isTimerServiceApplicable()}
     */
    @Override
    public boolean isTimerServiceRequired() {
        return false;
    }

    public ServiceName getDeploymentUnitServiceName() {
        return this.deploymentUnitServiceName;
    }
}
