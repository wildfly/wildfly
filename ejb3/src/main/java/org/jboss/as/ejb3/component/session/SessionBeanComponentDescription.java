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


import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBMethodDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.tx.CMTTxInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.ejb3.tx2.spi.TransactionalComponent;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
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
import java.util.Collections;
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

    private Map<String, MethodIntf> viewTypes = new HashMap<String, MethodIntf>();

    /**
     * The {@link javax.ejb.ConcurrencyManagementType} for this bean
     */
    private ConcurrencyManagementType concurrencyManagementType;

    /**
     * The bean level {@link LockType} for this bean.
     */
    private LockType beanLevelLockType;

    /**
     * The bean level {@link AccessTimeout} for this bean.
     */
    private AccessTimeout beanLevelAccessTimeout;

    /**
     * The {@link LockType} applicable for a specific bean methods.
     */
    private Map<EJBMethodDescription, LockType> methodLockTypes = new ConcurrentHashMap<EJBMethodDescription, LockType>();

    /**
     * The {@link AccessTimeout} applicable for a specific bean methods.
     */
    private Map<EJBMethodDescription, AccessTimeout> methodAccessTimeouts = new ConcurrentHashMap<EJBMethodDescription, AccessTimeout>();

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
        addDependency(SessionBeanComponent.ASYNC_EXECUTOR_SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
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
        registerView(getEJBClassName(), MethodIntf.LOCAL);
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

    public void addRemoteBusinessInterfaceViews(final Collection<String> classNames) {
        for (final String viewClassName : classNames) {
            assertNoLocalView(viewClassName);
            registerView(viewClassName, MethodIntf.REMOTE);
        }
    }

    private void assertNoRemoteView(final String viewClassName) {
        if (viewTypes.get(viewClassName) == MethodIntf.REMOTE) {
            throw new IllegalStateException("[EJB 3.1 spec, section 4.9.7] - Can't add view class: " + viewClassName
                    + " as local view since it's already marked as remote view for bean: " + getEJBName());
        }
    }

    private void assertNoLocalView(final String viewClassName) {
        if (viewTypes.get(viewClassName) == MethodIntf.LOCAL) {
            throw new IllegalStateException("[EJB 3.1 spec, section 4.9.7] - Can't add view class: " + viewClassName
                    + " as remote view since it's already marked as local view for bean: " + getEJBName());
        }
    }

    private void registerView(final String viewClassName, final MethodIntf viewType) {
        // add it to our map
        viewTypes.put(viewClassName, viewType);
        // setup the ViewDescription
        final ViewDescription viewDescription = new ViewDescription(this, viewClassName);
        getViews().add(viewDescription);
        // setup server side view interceptors
        setupViewInterceptors(viewDescription);
        // setup client side view interceptors
        setupClientViewInterceptors(viewDescription);
    }

    @Override
    public MethodIntf getMethodIntf(String viewClassName) {
        MethodIntf methodIntf = viewTypes.get(viewClassName);
        assert methodIntf != null : "no view type known for " + viewClassName;
        return methodIntf;
    }

    public boolean hasNoInterfaceView() {
        return this.noInterfaceViewPresent;
    }

    /**
     * Sets the {@link javax.ejb.LockType} applicable for the bean.
     *
     * @param locktype The lock type applicable for the bean
     * @throws IllegalArgumentException If the bean has already been marked for a different {@link javax.ejb.LockType} than the one passed
     */
    public void setBeanLevelLockType(LockType locktype) {
        if (this.beanLevelLockType != null && this.beanLevelLockType != locktype) {
            throw new IllegalArgumentException(this.getEJBName() + " bean has already been marked for " + this.beanLevelLockType + " lock type. Cannot change it to " + locktype);
        }
        this.beanLevelLockType = locktype;
    }

    /**
     * Returns the {@link LockType} applicable for the bean.
     *
     * @return
     */
    public LockType getBeanLevelLockType() {
        return this.beanLevelLockType;
    }

    /**
     * Sets the {@link LockType} for the specific bean method
     *
     * @param lockType The applicable lock type for the method
     * @param method   The method
     */
    public void setLockType(LockType lockType, EJBMethodDescription method) {
        this.methodLockTypes.put(method, lockType);
    }

    public Map<EJBMethodDescription, LockType> getMethodApplicableLockTypes() {
        return Collections.unmodifiableMap(this.methodLockTypes);
    }

    /**
     * Returns the {@link AccessTimeout} applicable for the bean.
     *
     * @return
     */
    public AccessTimeout getBeanLevelAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    /**
     * Sets the {@link javax.ejb.AccessTimeout} applicable for the bean.
     *
     * @param accessTimeout The access timeout applicable for the bean
     * @throws IllegalArgumentException If the bean has already been marked for a different {@link javax.ejb.AccessTimeout} than the one passed
     */
    public void setBeanLevelAccessTimeout(AccessTimeout accessTimeout) {
        if (this.beanLevelAccessTimeout != null && this.beanLevelAccessTimeout != accessTimeout) {
            throw new IllegalArgumentException(this.getEJBName() + " bean has already been marked for " + this.beanLevelAccessTimeout + " access timeout. Cannot change it to " + accessTimeout);
        }
        this.beanLevelAccessTimeout = accessTimeout;
    }

    /**
     * Sets the {@link AccessTimeout} for the specific bean method
     *
     * @param accessTimeout The applicable access timeout for the method
     * @param method        The method
     */
    public void setAccessTimeout(AccessTimeout accessTimeout, EJBMethodDescription method) {
        this.methodAccessTimeouts.put(method, accessTimeout);
    }

    public Map<EJBMethodDescription, AccessTimeout> getMethodApplicableAccessTimeouts() {
        return Collections.unmodifiableMap(this.methodAccessTimeouts);
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

        // current invocation

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
                    configuration.addViewInterceptor(new ComponentInterceptorFactory() {
                                @Override
                                protected Interceptor create(Component component, InterceptorFactoryContext context) {
                                    if (!(component instanceof TransactionalComponent)) {
                                        throw new IllegalArgumentException("Component " + component + " with component class: " + component.getComponentClass() +
                                                " isn't a transactional component. Tx interceptors cannot be applied");
                                    }
                                    return new CMTTxInterceptor((TransactionalComponent) component);
                                }
                            }, InterceptorOrder.View.TRANSACTION_INTERCEPTOR);
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
                if (SessionBean.class.isAssignableFrom(configuration.getComponentClass())) {

                    configuration.addPostConstructInterceptor(SessionBeanSessionContextInjectionInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.RESOURCE_INJECTION_INTERCEPTORS);
                }
                configuration.addPostConstructInterceptor(SessionInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
                configuration.addPreDestroyInterceptor(SessionInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPreDestroy.EJB_SESSION_CONTEXT_INTERCEPTOR);
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
