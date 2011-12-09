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
package org.jboss.as.ejb3.component;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;
import org.jboss.as.ejb3.tx.TransactionMethodAttribute;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponent extends BasicComponent {
    private static final Logger log = Logger.getLogger(EJBComponent.class);

    private static final ApplicationExceptionDetails APPLICATION_EXCEPTION = new ApplicationExceptionDetails("java.lang.Exception", true, false);

    private final Map<MethodTransactionAttributeKey, TransactionMethodAttribute> txAttrs;

    private final EJBUtilities utilities;
    private final boolean isBeanManagedTransaction;
    private final Map<Class<?>, ApplicationExceptionDetails> applicationExceptions;
    private final EJBSecurityMetaData securityMetaData;
    private final Map<String, ServiceName> viewServices;
    private final ServiceName ejbLocalHomeViewServiceName;
    private final ServiceName ejbHomeViewServiceName;
    private final ServiceName ejbObjectViewServiceName;
    private final ServiceName ejbLocalObjectViewServiceName;

    private final TimerService timerService;
    private final Map<Method, InterceptorFactory> timeoutInterceptors;
    private final Method timeoutMethod;
    private final String applicationName;
    private final String earApplicationName;
    private final String moduleName;
    private final String distinctName;
    private final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected EJBComponent(final EJBComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);


        this.applicationExceptions = Collections.unmodifiableMap(ejbComponentCreateService.getApplicationExceptions().getApplicationExceptions());

        this.utilities = ejbComponentCreateService.getEJBUtilities();

        final Map<MethodTransactionAttributeKey, TransactionMethodAttribute> txAttrs = ejbComponentCreateService.getTxAttrs();
        if (txAttrs == null || txAttrs.isEmpty()) {
            this.txAttrs = Collections.emptyMap();
        } else {
            this.txAttrs = txAttrs;
        }
        isBeanManagedTransaction = TransactionManagementType.BEAN.equals(ejbComponentCreateService.getTransactionManagementType());

        // security metadata
        this.securityMetaData = ejbComponentCreateService.getSecurityMetaData();
        this.viewServices = ejbComponentCreateService.getViewServices();
        this.timerService = ejbComponentCreateService.getTimerService();
        this.timeoutMethod = ejbComponentCreateService.getTimeoutMethod();
        this.ejbLocalHomeViewServiceName = ejbComponentCreateService.getEjbLocalHome();
        this.ejbHomeViewServiceName = ejbComponentCreateService.getEjbHome();
        this.applicationName = ejbComponentCreateService.getApplicationName();
        this.earApplicationName = ejbComponentCreateService.getEarApplicationName();
        this.distinctName = ejbComponentCreateService.getDistinctName();
        this.moduleName = ejbComponentCreateService.getModuleName();
        this.ejbObjectViewServiceName = ejbComponentCreateService.getEjbObject();
        this.ejbLocalObjectViewServiceName = ejbComponentCreateService.getEjbLocalObject();

        this.ejbRemoteTransactionsRepository = ejbComponentCreateService.getEJBRemoteTransactionsRepository();
        this.timeoutInterceptors = Collections.unmodifiableMap(ejbComponentCreateService.getTimeoutInterceptors());
    }

    protected <T> T createViewInstanceProxy(final Class<T> viewInterface, final Map<Object, Object> contextData) {
        if (viewInterface == null)
            throw new IllegalArgumentException("View interface is null");
        if (viewServices.containsKey(viewInterface.getName())) {
            final ServiceName serviceName = viewServices.get(viewInterface.getName());
            return createViewInstanceProxy(viewInterface, contextData, serviceName);
        } else {
            throw new IllegalStateException("View of type " + viewInterface + " not found on bean " + this);
        }
    }

    protected <T> T createViewInstanceProxy(final Class<T> viewInterface, final Map<Object, Object> contextData, final ServiceName serviceName) {
        final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getRequiredService(serviceName);
        final ComponentView view = (ComponentView) serviceController.getValue();
        final ManagedReference instance;
        try {
            instance = view.createInstance(contextData);
        } catch (Exception e) {
            //TODO: do we need to let the exception propagate here?
            throw new RuntimeException(e);
        }
        return viewInterface.cast(instance.getInstance());
    }

    public ApplicationExceptionDetails getApplicationException(Class<?> exceptionClass, Method invokedMethod) {
        ApplicationExceptionDetails applicationException = this.applicationExceptions.get(exceptionClass);
        if (applicationException != null) {
            return applicationException;
        }
        // Check if the super class of the passed exception class, is an application exception.
        Class<?> superClass = exceptionClass.getSuperclass();
        while (superClass != null && !(superClass.equals(Exception.class) || superClass.equals(Object.class))) {
            applicationException = this.applicationExceptions.get(superClass);
            // check whether the "inherited" attribute is set. A subclass of an application exception
            // is an application exception only if the inherited attribute on the parent application exception
            // is set to true.
            if (applicationException != null) {
                if (applicationException.isInherited()) {
                    return applicationException;
                }
                // Once we find a super class which is an application exception,
                // we just stop there (no need to check the grand super class), irrespective of whether the "inherited"
                // is true or false
                return null; // not an application exception, so return null
            }
            // move to next super class
            superClass = superClass.getSuperclass();
        }
        // AS7-1317: examine the throws clause of the method
        // An unchecked-exception is only an application exception if annotated (or described) as such.
        // (see EJB 3.1 FR 14.2.1)
        if (RuntimeException.class.isAssignableFrom(exceptionClass) || Error.class.isAssignableFrom(exceptionClass))
            return null;
        if (invokedMethod != null) {
            final Class<?>[] exceptionTypes = invokedMethod.getExceptionTypes();
            for (Class<?> type : exceptionTypes) {
                if (type.isAssignableFrom(exceptionClass))
                    return APPLICATION_EXCEPTION;
            }
        }
        // not an application exception, so return null.
        return null;
    }

    public Principal getCallerPrincipal() {
        return utilities.getSecurityManager().getCallerPrincipal();
    }

    protected TransactionAttributeType getCurrentTransactionAttribute() {

        final InterceptorContext invocation = CurrentInvocationContext.get();
        final MethodIntf methodIntf = MethodIntfHelper.of(invocation);
        return getTransactionAttribute(methodIntf, invocation.getMethod()).getType();
    }

    public EJBHome getEJBHome() throws IllegalStateException {
        if (ejbHomeViewServiceName == null) {
            throw MESSAGES.beanHomeInterfaceIsNull(getComponentName());
        }
        final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getRequiredService(ejbHomeViewServiceName);
        final ComponentView view = (ComponentView) serviceController.getValue();
        final String locatorAppName = earApplicationName == null ? "" : earApplicationName;
        return EJBClient.createProxy(new EJBHomeLocator<EJBHome>((Class<EJBHome>) view.getViewClass(), locatorAppName, moduleName, getComponentName(), distinctName));
    }

    public Class<?> getEjbObjectType() {
        if (ejbObjectViewServiceName == null) {
            return null;
        }
        final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getRequiredService(ejbObjectViewServiceName);
        final ComponentView view = (ComponentView) serviceController.getValue();
        return view.getViewClass();
    }

    public Class<?> getEjbLocalObjectType() {
        if (ejbLocalObjectViewServiceName == null) {
            return null;
        }
        final ServiceController<?> serviceController = CurrentServiceContainer.getServiceContainer().getRequiredService(ejbLocalObjectViewServiceName);
        final ComponentView view = (ComponentView) serviceController.getValue();
        return view.getViewClass();
    }

    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        if (ejbLocalHomeViewServiceName == null) {
            throw MESSAGES.beanLocalHomeInterfaceIsNull(getComponentName());
        }
        return createViewInstanceProxy(EJBLocalHome.class, Collections.emptyMap(), ejbLocalHomeViewServiceName);
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        if (isBeanManagedTransaction()) {
            throw MESSAGES.failToCallgetRollbackOnly();
        }
        try {
            TransactionManager tm = this.getTransactionManager();

            // The getRollbackOnly method should be used only in the context of a transaction.
            if (tm.getTransaction() == null) {
                throw MESSAGES.failToCallgetRollbackOnlyOnNoneTransaction();
            }

            // EJBTHREE-805, consider an asynchronous rollback due to timeout
            // This is counter to EJB 3.1 where an asynchronous call does not inherit the transaction context!

            int status = tm.getStatus();
            if (log.isTraceEnabled()) {
                ROOT_LOGGER.trace("Current transaction status is " + status);
            }
            switch (status) {
                case Status.STATUS_COMMITTED:
                case Status.STATUS_ROLLEDBACK:
                    throw MESSAGES.failToCallgetRollbackOnlyAfterTxcompleted();
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_ROLLING_BACK:
                    return true;
            }
            return false;
        } catch (SystemException se) {
            ROOT_LOGGER.getTxManagerStatusFailed(se);
            return true;
        }
    }

    public SimpleSecurityManager getSecurityManager() {
        return utilities.getSecurityManager();
    }

    public TimerService getTimerService() throws IllegalStateException {
        return timerService;
    }

    /**
     * @see EJBComponent#getTransactionAttribute(MethodIntf, java.lang.reflect.Method)
     */
    @Deprecated
    public TransactionAttributeType getTransactionAttributeType(final MethodIntf methodIntf, final Method method) {
        return getTransactionAttribute(methodIntf, method).getType();
    }

    public TransactionMethodAttribute getTransactionAttribute(final MethodIntf methodIntf, final Method method) {
        TransactionMethodAttribute txAttr = txAttrs.get(new MethodTransactionAttributeKey(methodIntf, MethodIdentifier.getIdentifierForMethod(method)));
        //fall back to type bean if not found
        if (txAttr == null && methodIntf != MethodIntf.BEAN) {
            txAttr = txAttrs.get(new MethodTransactionAttributeKey(MethodIntf.BEAN, MethodIdentifier.getIdentifierForMethod(method)));
        }
        if (txAttr == null)
            return TransactionMethodAttribute.REQUIRED;
        return txAttr;
    }

    public TransactionManager getTransactionManager() {
        return utilities.getTransactionManager();
    }

    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return utilities.getTransactionSynchronizationRegistry();
    }

    public UserTransaction getUserTransaction() throws IllegalStateException {
        if (!isBeanManagedTransaction())
            throw MESSAGES.failToCallIsBeanManagedTransaction();
        return utilities.getUserTransaction();
    }

    private boolean isBeanManagedTransaction() {
        return isBeanManagedTransaction;
    }

    public boolean isCallerInRole(final String roleName) throws IllegalStateException {
        return utilities.getSecurityManager().isCallerInRole(securityMetaData.getSecurityRoles(), securityMetaData.getSecurityRoleLinks(), roleName);
    }

    public Object lookup(String name) throws IllegalArgumentException {
        if (name == null) {
            throw MESSAGES.jndiNameCannotBeNull();
        }
        final NamespaceContextSelector namespaceContextSelector = NamespaceContextSelector.getCurrentSelector();
        if (namespaceContextSelector == null) {
            throw MESSAGES.noNamespaceContextSelectorAvailable(name);
        }
        Context jndiContext = null;
        String namespaceStrippedJndiName = name;
        // get the appropriate JNDI context and strip the lookup jndi name of the component namespace prefix
        if (name.startsWith("java:app/")) {
            jndiContext = namespaceContextSelector.getContext("app");
            namespaceStrippedJndiName = name.substring("java:app/".length());
        } else if (name.startsWith("java:module/")) {
            jndiContext = namespaceContextSelector.getContext("module");
            namespaceStrippedJndiName = name.substring("java:module/".length());
        } else if (name.startsWith("java:comp/")) {
            jndiContext = namespaceContextSelector.getContext("comp");
            namespaceStrippedJndiName = name.substring("java:comp/".length());
        } else if (!name.startsWith("java:")) { // if it *doesn't* start with java: prefix, then default it to java:comp
            jndiContext = namespaceContextSelector.getContext("comp");
            // no need to strip the name since it doesn't start with java: prefix.
            // Also prefix the "env/" to the jndi name, since a lookup without a java: namespace prefix is effectively
            // a lookup under java:comp/env/<jndi-name>
            namespaceStrippedJndiName = "env/" + name;
        } else if (name.startsWith("java:global/")) {
            // Do *not* strip the jndi name of the prefix because java:global is a global context and doesn't specifically
            // belong to the component's ENC, and hence *isn't* a component ENC relative name and has to be looked up
            // with the absolute name (including the java:global prefix)
            try {
                jndiContext = new InitialContext();
            } catch (NamingException ne) {
                throw MESSAGES.failToLookupJNDI(name, ne);
            }
        } else {
            throw MESSAGES.failToLookupJNDINameSpace(name);
        }
        ROOT_LOGGER.debug("Looking up " + namespaceStrippedJndiName + " in jndi context: " + jndiContext);
        try {
            return jndiContext.lookup(namespaceStrippedJndiName);
        } catch (NamingException ne) {
            throw MESSAGES.failToLookupStrippedJNDI(namespaceContextSelector, jndiContext, ne);
        }
    }

    public void setRollbackOnly() throws IllegalStateException {
        if (isBeanManagedTransaction()) {
            throw MESSAGES.failToCallSetRollbackOnlyOnNoneCMB();
        }
        try {
            // get the transaction manager
            TransactionManager tm = getTransactionManager();
            // check if there's a tx in progress. If not, then it's an error to call setRollbackOnly()
            if (tm.getTransaction() == null) {
                throw MESSAGES.failToCallSetRollbackOnlyWithNoTx();
            }
            // set rollback
            tm.setRollbackOnly();
        } catch (SystemException se) {
            ROOT_LOGGER.setRollbackOnlyFailed(se);
        }
    }

    public EJBSecurityMetaData getSecurityMetaData() {
        return this.securityMetaData;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
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

    public ServiceName getEjbLocalObjectViewServiceName() {
        return ejbLocalObjectViewServiceName;
    }

    public ServiceName getEjbLocalHomeViewServiceName() {
        return ejbLocalHomeViewServiceName;
    }

    public ServiceName getEjbHomeViewServiceName() {
        return ejbHomeViewServiceName;
    }

    public ServiceName getEjbObjectViewServiceName() {
        return ejbObjectViewServiceName;
    }

    public Map<Method, InterceptorFactory> getTimeoutInterceptors() {
        return timeoutInterceptors;
    }

    /**
     * Returns the {@link EJBRemoteTransactionsRepository} if there is atleast one remote view (either
     * ejb3.x business remote, ejb2.x remote component or home view) is exposed. Else returns null.
     *
     * @return
     */
    public EJBRemoteTransactionsRepository getEjbRemoteTransactionsRepository() {
        return this.ejbRemoteTransactionsRepository;
    }
}
