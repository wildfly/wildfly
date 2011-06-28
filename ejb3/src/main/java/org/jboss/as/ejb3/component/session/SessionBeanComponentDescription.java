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

package org.jboss.as.ejb3.component.session;


import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import javax.ejb.SessionBean;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jaikiran Pai
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class SessionBeanComponentDescription extends EJBComponentDescription {

    private static final Logger logger = Logger.getLogger(SessionBeanComponentDescription.class);

    /**
     * Flag marking the presence/absence of a no-interface view on the session bean
     */
    private boolean noInterfaceViewPresent;

    /**
     * The {@link javax.ejb.ConcurrencyManagementType} for this bean
     */
    private ConcurrencyManagementType concurrencyManagementType;

    /**
     * Map of class name to default {@link LockType} for this bean.
     */
    private Map<String, LockType> beanLevelLockType = new HashMap<String, LockType>();

    /**
     * Map of class name to default {@link AccessTimeout} for this component.
     */
    private Map<String, AccessTimeout> beanLevelAccessTimeout = new HashMap<String, AccessTimeout>();

    /**
     * The {@link LockType} applicable for a specific bean methods.
     */
    private Map<MethodIdentifier, LockType> methodLockTypes = new ConcurrentHashMap<MethodIdentifier, LockType>();

    /**
     * The {@link AccessTimeout} applicable for a specific bean methods.
     */
    private Map<MethodIdentifier, AccessTimeout> methodAccessTimeouts = new ConcurrentHashMap<MethodIdentifier, AccessTimeout>();

    /**
     * Methods on the component marked as @Asynchronous
     */
    private final Set<MethodIdentifier> asynchronousMethods = new HashSet<MethodIdentifier>();

    /**
     * Views the component marked as @Asynchronous
     */
    private final Set<String> asynchronousViews = new HashSet<String>();


    /**
     * mapped-name of the session bean
     */
    private String mappedName;

    public enum SessionBeanType {
        STATELESS,
        STATEFUL,
        SINGLETON
    }

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module description
     */
    public SessionBeanComponentDescription(final String componentName, final String componentClassName,
                                           final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
        // TODO: AS7-447
        //addDependency(SessionBeanComponent.ASYNC_EXECUTOR_SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
        // setSessionContext() method invocation interceptor
        this.addSetSessionContextMethodInvocationInterceptor();
    }

    /**
     * Returns true if this session bean component type allows concurrent access to the component instances.
     * <p/>
     * For example: Singleton and stateful beans allow concurrent access to the bean instances, whereas stateless beans don't.
     *
     * @return
     */
    public abstract boolean allowsConcurrentAccess();

    public void addLocalBusinessInterfaceViews(final Collection<String> classNames) {
        for (final String viewClassName : classNames) {
            assertNoRemoteView(viewClassName);
            registerView(viewClassName, MethodIntf.LOCAL);
        }
    }

    public void addLocalBusinessInterfaceViews(final String... classNames) {
        addLocalBusinessInterfaceViews(Arrays.asList(classNames));
    }

    public void addNoInterfaceView() {
        noInterfaceViewPresent = true;
        final ViewDescription viewDescription = registerView(getEJBClassName(), MethodIntf.LOCAL);
        //set up interceptor for non-business methods
        viewDescription.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                for (final Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if (!Modifier.isPublic(method.getModifiers())) {
                        configuration.addViewInterceptor(method, new ImmediateInterceptorFactory(new NotBusinessMethodInterceptor(method)), InterceptorOrder.View.NOT_BUSINESS_METHOD);
                    }
                }
            }
        });
    }

    public EJBViewDescription addWebserviceEndpointView() {
        return registerView(getEJBClassName(), MethodIntf.SERVICE_ENDPOINT);
    }

    public void addRemoteBusinessInterfaceViews(final Collection<String> classNames) {
        for (final String viewClassName : classNames) {
            assertNoLocalView(viewClassName);
            registerView(viewClassName, MethodIntf.REMOTE);
        }
    }

    private void assertNoRemoteView(final String viewClassName) {
        EJBViewDescription ejbView = null;
        for (final ViewDescription view : getViews()) {
            ejbView = (EJBViewDescription) view;
            if (viewClassName.equals(ejbView.getViewClassName()) && ejbView.getMethodIntf() == MethodIntf.REMOTE) {
                throw new IllegalStateException("[EJB 3.1 spec, section 4.9.7] - Can't add view class: " + viewClassName
                        + " as local view since it's already marked as remote view for bean: " + getEJBName());
            }
        }
    }

    private void assertNoLocalView(final String viewClassName) {
        EJBViewDescription ejbView = null;
        for (final ViewDescription view : getViews()) {
            ejbView = (EJBViewDescription) view;
            if (viewClassName.equals(ejbView.getViewClassName()) && ejbView.getMethodIntf() == MethodIntf.LOCAL) {
                throw new IllegalStateException("[EJB 3.1 spec, section 4.9.7] - Can't add view class: " + viewClassName
                        + " as remote view since it's already marked as local view for bean: " + getEJBName());
            }
        }
    }

    private EJBViewDescription registerView(final String viewClassName, final MethodIntf viewType) {
        // setup the ViewDescription
        final EJBViewDescription viewDescription = new EJBViewDescription(this, viewClassName, viewType);
        getViews().add(viewDescription);
        // setup server side view interceptors
        setupViewInterceptors(viewDescription);
        // setup client side view interceptors
        setupClientViewInterceptors(viewDescription);
        // return created view
        return viewDescription;
    }

    public boolean hasNoInterfaceView() {
        return this.noInterfaceViewPresent;
    }

    /**
     * Sets the {@link javax.ejb.LockType} applicable for the bean.
     *
     * @param className The class that has the annotation
     * @param locktype  The lock type applicable for the bean
     */
    public void setBeanLevelLockType(String className, LockType locktype) {
        this.beanLevelLockType.put(className, locktype);
    }

    /**
     * Returns the {@link LockType} applicable for the bean.
     *
     * @return
     */
    public Map<String, LockType> getBeanLevelLockType() {
        return this.beanLevelLockType;
    }

    /**
     * Sets the {@link LockType} for the specific bean method
     *
     * @param lockType The applicable lock type for the method
     * @param method   The method
     */
    public void setLockType(LockType lockType, MethodIdentifier method) {
        this.methodLockTypes.put(method, lockType);
    }

    public Map<MethodIdentifier, LockType> getMethodApplicableLockTypes() {
        return this.methodLockTypes;
    }

    /**
     * Returns the {@link AccessTimeout} applicable for the bean.
     *
     * @return
     */
    public Map<String, AccessTimeout> getBeanLevelAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    /**
     * Sets the {@link javax.ejb.AccessTimeout} applicable for the bean.
     *
     * @param accessTimeout The access timeout applicable for the class
     */
    public void setBeanLevelAccessTimeout(String className, AccessTimeout accessTimeout) {
        this.beanLevelAccessTimeout.put(className, accessTimeout);
    }

    /**
     * Sets the {@link AccessTimeout} for the specific bean method
     *
     * @param accessTimeout The applicable access timeout for the method
     * @param method        The method
     */
    public void setAccessTimeout(AccessTimeout accessTimeout, MethodIdentifier method) {
        this.methodAccessTimeouts.put(method, accessTimeout);
    }

    public Map<MethodIdentifier, AccessTimeout> getMethodApplicableAccessTimeouts() {
        return this.methodAccessTimeouts;
    }

    /**
     * Returns the concurrency management type for this bean.
     * <p/>
     * This method returns null if the concurrency management type hasn't explicitly been set on this
     * {@link SessionBeanComponentDescription}
     *
     * @return
     */
    public ConcurrencyManagementType getConcurrencyManagementType() {
        return this.concurrencyManagementType;
    }

    /**
     * Marks the bean for bean managed concurrency.
     *
     * @throws IllegalStateException If the bean has already been marked for a different concurrency management type
     */
    public void beanManagedConcurrency() {
        if (this.concurrencyManagementType != null && this.concurrencyManagementType != ConcurrencyManagementType.BEAN) {
            throw new IllegalStateException(this.getEJBName() + " bean has been marked for " + this.concurrencyManagementType + " cannot change it now!");
        }
        this.concurrencyManagementType = ConcurrencyManagementType.BEAN;
    }


    /**
     * Marks this bean for container managed concurrency.
     *
     * @throws IllegalStateException If the bean has already been marked for a different concurrency management type
     */
    public void containerManagedConcurrency() {
        if (this.concurrencyManagementType != null && this.concurrencyManagementType != ConcurrencyManagementType.CONTAINER) {
            throw new IllegalStateException(this.getEJBName() + " bean has been marked for " + this.concurrencyManagementType + " cannot change it now!");
        }
        this.concurrencyManagementType = ConcurrencyManagementType.CONTAINER;

    }

    /**
     * Returns the mapped-name of this bean
     *
     * @return
     */
    public String getMappedName() {
        return this.mappedName;
    }

    /**
     * Sets the mapped-name for this bean
     *
     * @param mappedName
     */
    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
    }

    /**
     * Add an asynchronous method.
     *
     * @param methodIdentifier The identifier for an async method
     */
    public void addAsynchronousMethod(final MethodIdentifier methodIdentifier) {
        asynchronousMethods.add(methodIdentifier);
    }

    /**
     * Set an entire view's asynchronous nature.  All business methods for the view will be asynchronous.
     *
     * @param viewName The view name
     */
    public void addAsynchronousView(final String viewName) {
        asynchronousViews.add(viewName);
    }

    /**
     * Returns the type of the session bean
     *
     * @return
     */
    public abstract SessionBeanType getSessionBeanType();

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let super do it's job first
        super.setupViewInterceptors(view);

        // tx management interceptor(s)
        addTxManagementInterceptorForView(view);

    }

    /**
     * Sets up the transaction management interceptor for all methods of the passed view.
     *
     * @param view The EJB bean view
     */
    protected static void addTxManagementInterceptorForView(ViewDescription view) {
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
    }

    @Override
    protected void addCurrentInvocationContextFactory() {
        // add the current invocation context interceptor at the beginning of the component instance post construct chain
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(SessionInvocationContextInterceptor.LIFECYCLE_FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
                configuration.addPreDestroyInterceptor(SessionInvocationContextInterceptor.LIFECYCLE_FACTORY, InterceptorOrder.ComponentPreDestroy.EJB_SESSION_CONTEXT_INTERCEPTOR);
            }
        });
    }

    private void addSetSessionContextMethodInvocationInterceptor() {
        // add the setSessionContext(SessionContext) method invocation interceptor for session bean implementing the javax.ejb.SessionContext
        // interface
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                if (SessionBean.class.isAssignableFrom(configuration.getComponentClass())) {
                    configuration.addPostConstructInterceptor(SessionBeanSetSessionContextMethodInvocationInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SET_SESSION_CONTEXT_METHOD_INVOCATION_INTERCEPTOR);
                }
            }
        });
    }


    @Override
    protected void addCurrentInvocationContextFactory(ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(SessionInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
            }
        });

    }

    @Override
    public boolean isSingleton() {
        return getSessionBeanType() == SessionBeanType.SINGLETON;
    }

    @Override
    public boolean isStateful() {
        return getSessionBeanType() == SessionBeanType.STATEFUL;
    }

    @Override
    public boolean isStateless() {
        return getSessionBeanType() == SessionBeanType.STATELESS;
    }

}
