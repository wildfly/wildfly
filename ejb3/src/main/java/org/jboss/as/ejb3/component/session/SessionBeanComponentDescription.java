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

import org.jboss.as.ee.component.AbstractComponentConfiguration;
import org.jboss.as.ejb3.PrimitiveClassLoaderUtil;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBMethodDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jaikiran Pai
 */
public abstract class SessionBeanComponentDescription extends EJBComponentDescription {

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
     * mapped-name of the session bean
     */
    private String mappedName;

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param moduleName         the module name
     * @param applicationName    the application name
     */
    public SessionBeanComponentDescription(final String componentName, final String componentClassName, final String moduleName, final String applicationName) {
        super(componentName, componentClassName, moduleName, applicationName);
    }

    /**
     * Returns true if this session bean component type allows concurrent access to the component instances.
     * <p/>
     * For example: Singleton and stateful beans allow concurrent access to the bean instances, whereas stateless beans don't.
     *
     * @return
     */
    public abstract boolean allowsConcurrentAccess();

    public void addLocalBusinessInterfaceViews(Collection<String> classNames) {
        this.getViewClassNames().addAll(classNames);
        for (String viewClassName : classNames) {
            viewTypes.put(viewClassName, MethodIntf.LOCAL);
        }
    }

    public void addLocalBusinessInterfaceViews(final String... classNames) {
        addLocalBusinessInterfaceViews(Arrays.asList(classNames));
    }

    public void addNoInterfaceView() {
        this.noInterfaceViewPresent = true;
        this.getViewClassNames().add(this.getEJBClassName());
        viewTypes.put(getEJBClassName(), MethodIntf.LOCAL);
    }

    public void addRemoteBusinessInterfaceViews(final Collection<String> classNames) {
        this.getViewClassNames().addAll(classNames);
        for (String viewClassName : classNames) {
            viewTypes.put(viewClassName, MethodIntf.REMOTE);
        }
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
     * Sets the {@link LockType} for the specific bean method represented by the <code>methodName</code> and <code>methodParamTypes</code>
     *
     * @param lockType The applicable lock type for the method
     * @param method   The method
     */
    public void setLockType(LockType lockType, EJBMethodDescription method) {
        this.methodLockTypes.put(method, lockType);
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
     * Sets the {@link AccessTimeout} for the specific bean method represented by the <code>methodName</code> and <code>methodParamTypes</code>
     *
     * @param accessTimeout The applicable access timeout for the method
     * @param method        The method
     */
    public void setAccessTimeout(AccessTimeout accessTimeout, EJBMethodDescription method) {
        this.methodAccessTimeouts.put(method, accessTimeout);
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

    @Override
    protected void prepareComponentConfiguration(AbstractComponentConfiguration configuration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // let super do it's job first
        super.prepareComponentConfiguration(configuration, phaseContext);

        SessionBeanComponentConfiguration sessionBeanComponentConfiguration = (SessionBeanComponentConfiguration) configuration;
        // update the SessionBeanConfiguration with the method level LockType info
        this.prepareLockConfiguration(sessionBeanComponentConfiguration, phaseContext);
        // update the SessionBeanConfiguration with the method level @AccessTimeout info
        this.prepareAccessTimeoutConfiguration(sessionBeanComponentConfiguration, phaseContext);
    }

    private void prepareAccessTimeoutConfiguration(SessionBeanComponentConfiguration sessionBeanComponentConfiguration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ClassLoader beanClassLoader = sessionBeanComponentConfiguration.getComponentClass().getClassLoader();
        Map<EJBBusinessMethod, AccessTimeout> methodApplicableAccessTimeouts = new HashMap();
        for (Map.Entry<EJBMethodDescription, AccessTimeout> entry : this.methodAccessTimeouts.entrySet()) {
            EJBMethodDescription method = entry.getKey();
            try {
                EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(method, beanClassLoader);
                methodApplicableAccessTimeouts.put(ejbMethod, entry.getValue());
            } catch (ClassNotFoundException cnfe) {
                throw new DeploymentUnitProcessingException("Could not process @AccessTimeout configurations due to exception: ", cnfe);
            }

        }
        // add it to the SessionBeanConfiguration
        sessionBeanComponentConfiguration.setMethodApplicableAccessTimeout(methodApplicableAccessTimeouts);

    }

    private void prepareLockConfiguration(SessionBeanComponentConfiguration sessionBeanComponentConfiguration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ClassLoader beanClassLoader = sessionBeanComponentConfiguration.getComponentClass().getClassLoader();
        Map<EJBBusinessMethod, LockType> methodApplicableLockTypes = new HashMap();
        for (Map.Entry<EJBMethodDescription, LockType> entry : this.methodLockTypes.entrySet()) {
            EJBMethodDescription method = entry.getKey();
            try {
                EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(method, beanClassLoader);
                methodApplicableLockTypes.put(ejbMethod, entry.getValue());

            } catch (ClassNotFoundException cnfe) {
                throw new DeploymentUnitProcessingException("Could not process LockType configurations due to exception: ", cnfe);
            }
        }
        // add the locktype to the session bean configuration
        sessionBeanComponentConfiguration.setMethodApplicableLockType(methodApplicableLockTypes);

    }

    private EJBBusinessMethod getEJBBusinessMethod(EJBMethodDescription method, ClassLoader classLoader) throws ClassNotFoundException {
        String methodName = method.getMethodName();
        String[] types = method.getMethodParams();
        if (types == null || types.length == 0) {
            return new EJBBusinessMethod(methodName, new Class<?>[0]);
        }
        Class<?>[] paramTypes = new Class<?>[types.length];
        int i = 0;
        for (String type : types) {
            paramTypes[i++] = PrimitiveClassLoaderUtil.loadClass(type.toString(), classLoader);
        }
        return new EJBBusinessMethod(methodName, paramTypes);
    }

}
