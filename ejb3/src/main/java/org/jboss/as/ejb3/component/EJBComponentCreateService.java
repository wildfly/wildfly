/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import javax.ejb.TimerService;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * @author Jaikiran Pai
 */
public class EJBComponentCreateService extends BasicComponentCreateService {

    private final Map<MethodTransactionAttributeKey, TransactionAttributeType> txAttrs;
    private final Map<MethodTransactionAttributeKey, Boolean> txExplicitAttrs;

    private final Map<MethodTransactionAttributeKey, Integer> txTimeouts;

    private final TransactionManagementType transactionManagementType;

    private final ApplicationExceptions applicationExceptions;

    private final Map<String, ServiceName> viewServices;

    private final EJBSecurityMetaData securityMetaData;

    private final TimerService timerService;

    private final Map<Method, InterceptorFactory> timeoutInterceptors;

    private final Method timeoutMethod;

    private final ServiceName ejbLocalHome;
    private final ServiceName ejbHome;
    private final ServiceName ejbObject;
    private final ServiceName ejbLocalObject;


    private final String applicationName;
    private final String earApplicationName;
    private final String moduleName;
    private final String distinctName;
    private final String policyContextID;

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue = new InjectedValue<>();
    private final InjectedValue<UserTransaction> userTransactionInjectedValue = new InjectedValue<>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistryValue = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<ServerSecurityManager> serverSecurityManagerInjectedValue = new InjectedValue<>();
    private final InjectedValue<ControlPoint> controlPoint = new InjectedValue<>();
    private final InjectedValue<AtomicBoolean> exceptionLoggingEnabled = new InjectedValue<>();
    private final InjectedValue<ApplicationSecurityDomain> applicationSecurityDomain = new InjectedValue<>();
    private final InjectedValue<Function> identityOutflowFunction = new InjectedValue<>();
    private final InjectedValue<EJBSuspendHandlerService> ejbSuspendHandler = new InjectedValue<>();

    private final ShutDownInterceptorFactory shutDownInterceptorFactory;

    private final boolean securityRequired;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public EJBComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions applicationExceptions) {
        super(componentConfiguration);

        this.applicationExceptions = applicationExceptions;
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        this.transactionManagementType = ejbComponentDescription.getTransactionManagementType();

        this.timerService = ejbComponentDescription.getTimerService();
        this.policyContextID = ejbComponentDescription.getPolicyContextID();

        // CMTTx
        if (transactionManagementType.equals(TransactionManagementType.CONTAINER)) {
            this.txAttrs = new HashMap<MethodTransactionAttributeKey, TransactionAttributeType>();
            this.txTimeouts = new HashMap<MethodTransactionAttributeKey, Integer>();
            this.txExplicitAttrs = new HashMap<>();
        } else {
            this.txAttrs = null;
            this.txTimeouts = null;
            this.txExplicitAttrs = null;
        }
        // Setup the security metadata for the bean
        this.securityMetaData = new EJBSecurityMetaData(componentConfiguration);

        if (ejbComponentDescription.isTimerServiceRequired()) {
            Map<Method, InterceptorFactory> timeoutInterceptors = new IdentityHashMap<Method, InterceptorFactory>();
            for (Method method : componentConfiguration.getDefinedComponentMethods()) {
                if ((ejbComponentDescription.getTimeoutMethod() != null && ejbComponentDescription.getTimeoutMethod().equals(method)) ||
                        ejbComponentDescription.getScheduleMethods().containsKey(method)) {
                    final InterceptorFactory interceptorFactory = Interceptors.getChainedInterceptorFactory(componentConfiguration.getAroundTimeoutInterceptors(method));
                    timeoutInterceptors.put(method, interceptorFactory);
                }
            }
            this.timeoutInterceptors = timeoutInterceptors;
        } else {
            timeoutInterceptors = Collections.emptyMap();
        }

        List<ViewConfiguration> views = componentConfiguration.getViews();
        if (views != null) {
            for (ViewConfiguration view : views) {
                //TODO: Move this into a configurator
                final EJBViewConfiguration ejbView = (EJBViewConfiguration) view;
                final MethodIntf viewType = ejbView.getMethodIntf();
                for (Method method : view.getProxyFactory().getCachedMethods()) {
                    // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                    final Method componentMethod = getComponentMethod(componentConfiguration, method.getName(), method.getParameterTypes());
                    if (componentMethod != null) {
                        this.processTxAttr(ejbComponentDescription, viewType, componentMethod);
                    } else {
                        this.processTxAttr(ejbComponentDescription, viewType, method);
                    }
                }
            }
        }

        this.timeoutMethod = ejbComponentDescription.getTimeoutMethod();

        // FIXME: TODO: a temporary measure until EJBTHREE-2120 is fully resolved, let's create tx attribute map
        // for the component methods. Once the issue is resolved, we should get rid of this block and just rely on setting
        // up the tx attributes only for the views exposed by this component
        // AS7-899: We only want to process public methods of the proper sub-class. (getDefinedComponentMethods returns all in random order)
        // TODO: use ClassReflectionIndex (low prio, because we store the result without class name) (which is a bug: AS7-905)
        Set<Method> lifeCycle = new HashSet<>(componentConfiguration.getLifecycleMethods());
        for (Method method : componentConfiguration.getComponentClass().getMethods()) {
            this.processTxAttr(ejbComponentDescription, MethodIntf.BEAN, method);
            lifeCycle.remove(method);
        }
        //now handle non-public lifecycle methods declared on the bean class itself
        //see WFLY-4127
        for(Method method : lifeCycle)  {
            if(method.getDeclaringClass().equals(componentConfiguration.getComponentClass())) {
                this.processTxAttr(ejbComponentDescription, MethodIntf.BEAN, method);
            }
        }

        final HashMap<String, ServiceName> viewServices = new HashMap<String, ServiceName>();
        for (ViewDescription view : componentConfiguration.getComponentDescription().getViews()) {
            viewServices.put(view.getViewClassName(), view.getServiceName());
        }
        this.viewServices = viewServices;
        final EjbHomeViewDescription localHome = ejbComponentDescription.getEjbLocalHomeView();
        this.ejbLocalHome = localHome == null ? null : ejbComponentDescription.getEjbLocalHomeView().getServiceName();
        final EjbHomeViewDescription home = ejbComponentDescription.getEjbHomeView();
        this.ejbHome = home == null ? null : home.getServiceName();
        final EJBViewDescription ejbObject = ejbComponentDescription.getEjbRemoteView();
        this.ejbObject = ejbObject == null ? null : ejbObject.getServiceName();
        final EJBViewDescription ejbLocalObject = ejbComponentDescription.getEjbLocalView();
        this.ejbLocalObject = ejbLocalObject == null ? null : ejbLocalObject.getServiceName();
        this.applicationName = componentConfiguration.getApplicationName();
        this.earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
        this.moduleName = componentConfiguration.getModuleName();
        this.distinctName = componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName();
        this.shutDownInterceptorFactory = ejbComponentDescription.getShutDownInterceptorFactory();
        this.securityRequired = ejbComponentDescription.isSecurityRequired();
    }

    @Override
    protected boolean requiresInterceptors(final Method method, final ComponentConfiguration componentConfiguration) {
        if (super.requiresInterceptors(method, componentConfiguration)) {
            return true;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        if ((ejbComponentDescription.getTimeoutMethod() != null && ejbComponentDescription.getTimeoutMethod().equals(method)) ||
                ejbComponentDescription.getScheduleMethods().containsKey(method)) {
            return true;
        }
        return false;
    }

    private static Method getComponentMethod(final ComponentConfiguration componentConfiguration, final String name, final Class<?>[] parameterTypes) {
        try {
            return componentConfiguration.getComponentClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * @return
     * @deprecated {@link EJBUtilities} is deprecated post 7.2.0.Final version.
     */
    @Deprecated
    protected EJBUtilities getEJBUtilities() {
        // constructs
        final DeploymentUnit deploymentUnit = getDeploymentUnitInjector().getValue();
        final ServiceController<EJBUtilities> serviceController = (ServiceController<EJBUtilities>) deploymentUnit.getServiceRegistry().getRequiredService(EJBUtilities.SERVICE_NAME);
        return serviceController.getValue();
    }

    Map<MethodTransactionAttributeKey, TransactionAttributeType> getTxAttrs() {
        return txAttrs;
    }

    Map<MethodTransactionAttributeKey, Boolean> getExplicitTxAttrs() {
        return txExplicitAttrs;
    }

    Map<MethodTransactionAttributeKey, Integer> getTxTimeouts() {
        return txTimeouts;
    }

    TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    ApplicationExceptions getApplicationExceptions() {
        return this.applicationExceptions;
    }

    protected void processTxAttr(final EJBComponentDescription ejbComponentDescription, final MethodIntf methodIntf, final Method method) {
        if (this.getTransactionManagementType().equals(TransactionManagementType.BEAN)) {
            // it's a BMT bean
            return;
        }

        MethodIntf defaultMethodIntf = (ejbComponentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
        TransactionAttributeType txAttr = ejbComponentDescription.getTransactionAttributes().getAttribute(methodIntf, method, defaultMethodIntf);
        MethodTransactionAttributeKey key = new MethodTransactionAttributeKey(methodIntf, MethodIdentifier.getIdentifierForMethod(method));
        if(txAttr != null) {
            txAttrs.put(key, txAttr);
            txExplicitAttrs.put(key, ejbComponentDescription.getTransactionAttributes().isMethodLevel(methodIntf, method, defaultMethodIntf));
        }
        Integer txTimeout = ejbComponentDescription.getTransactionTimeouts().getAttribute(methodIntf, method, defaultMethodIntf);
        if (txTimeout != null) {
            txTimeouts.put(key, txTimeout);
        }
    }

    public Map<String, ServiceName> getViewServices() {
        return viewServices;
    }

    public EJBSecurityMetaData getSecurityMetaData() {
        return this.securityMetaData;
    }

    public Map<Method, InterceptorFactory> getTimeoutInterceptors() {
        return timeoutInterceptors;
    }

    public TimerService getTimerService() {
        return timerService;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
    }

    public ServiceName getEjbHome() {
        return ejbHome;
    }

    public ServiceName getEjbLocalHome() {
        return ejbLocalHome;
    }

    public ServiceName getEjbObject() {
        return ejbObject;
    }

    public ServiceName getEjbLocalObject() {
        return ejbLocalObject;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEarApplicationName() {
        return this.earApplicationName;
    }

    public String getDistinctName() {
        return distinctName;
    }

    public String getModuleName() {
        return moduleName;
    }

    Injector<UserTransaction> getUserTransactionInjector() {
        return this.userTransactionInjectedValue;
    }

    UserTransaction getUserTransaction() {
        return this.userTransactionInjectedValue.getValue();
    }

    public Injector<EJBSuspendHandlerService> getEJBSuspendHandlerInjector() {
        return this.ejbSuspendHandler;
    }

    EJBSuspendHandlerService getEJBSuspendHandler() {
        return this.ejbSuspendHandler.getValue();
    }

    ServerSecurityManager getServerSecurityManager() {
        return this.serverSecurityManagerInjectedValue.getOptionalValue();
    }

    Injector<ServerSecurityManager> getServerSecurityManagerInjector() {
        return this.serverSecurityManagerInjectedValue;
    }

    public ControlPoint getControlPoint() {
        return this.controlPoint.getOptionalValue();
    }

    public Injector<ControlPoint> getControlPointInjector() {
        return this.controlPoint;
    }

    public String getPolicyContextID() {
        return this.policyContextID;
    }

    InjectedValue<AtomicBoolean> getExceptionLoggingEnabledInjector() {
        return exceptionLoggingEnabled;
    }

    public AtomicBoolean getExceptionLoggingEnabled() {
        return exceptionLoggingEnabled.getValue();
    }

    Injector<ApplicationSecurityDomain> getApplicationSecurityDomainInjector() {
        return applicationSecurityDomain;
    }

    public ApplicationSecurityDomain getApplicationSecurityDomain() {
        return applicationSecurityDomain.getOptionalValue();
    }

    public SecurityDomain getSecurityDomain() {
        ApplicationSecurityDomain applicationSecurityDomain = getApplicationSecurityDomain();
        return applicationSecurityDomain != null ? applicationSecurityDomain.getSecurityDomain() : null;
    }

    public boolean isEnableJacc() {
        ApplicationSecurityDomain applicationSecurityDomain = getApplicationSecurityDomain();
        return applicationSecurityDomain != null ? applicationSecurityDomain.isEnableJacc() : false;
    }

    Injector<Function> getIdentityOutflowFunctionInjector() {
        return identityOutflowFunction;
    }

    public Function getIdentityOutflowFunction() {
        return identityOutflowFunction.getOptionalValue();
    }

    public ShutDownInterceptorFactory getShutDownInterceptorFactory() {
        return shutDownInterceptorFactory;
    }

    public boolean isSecurityRequired() {
        return securityRequired;
    }
}
