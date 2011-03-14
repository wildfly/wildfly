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

import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import java.util.Collection;
import java.util.Collections;
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
    private Map<EJBBusinessMethod, LockType> methodLockTypes = new ConcurrentHashMap<EJBBusinessMethod, LockType>();

    /**
     * The {@link AccessTimeout} applicable for a specific bean methods.
     */
    private Map<EJBBusinessMethod, AccessTimeout> methodAccessTimeouts = new ConcurrentHashMap<EJBBusinessMethod, AccessTimeout>();

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

    public void addNoInterfaceView() {
        this.noInterfaceViewPresent = true;
        this.getViewClassNames().add(this.getEJBClassName());
        viewTypes.put(getEJBClassName(), MethodIntf.LOCAL);
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
     * @param lockType         The applicable lock type for the method
     * @param methodName       The name of the method
     * @param methodParamTypes The method param types
     */
    public void setLockType(LockType lockType, String methodName, String... methodParamTypes) {
        EJBBusinessMethod ejbMethod = new EJBBusinessMethod(methodName, methodParamTypes);
        this.methodLockTypes.put(ejbMethod, lockType);
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
     * @param accessTimeout    The applicable access timeout for the method
     * @param methodName       The name of the method
     * @param methodParamTypes The method param types
     */
    public void setAccessTimeout(AccessTimeout accessTimeout, String methodName, String... methodParamTypes) {
        EJBBusinessMethod ejbMethod = new EJBBusinessMethod(methodName, methodParamTypes);
        this.methodAccessTimeouts.put(ejbMethod, accessTimeout);
    }

    /**
     * Returns a (unmodifiable) map of lock types applicable for the bean methods. The returned map will contain the
     * lock type for a method, <i>only</i> if the lock type has been explicitly specified for the bean method.
     * <p/>
     * Returns an empty map if there are no explicit method level lock types specified for the bean
     *
     * @return
     */
    public Map<EJBBusinessMethod, LockType> getMethodApplicableLockTypes() {
        return Collections.unmodifiableMap(this.methodLockTypes);
    }

    /**
     * Returns a (unmodifiable) map of {@link AccessTimeout} applicable for the bean methods. The returned map will contain the
     * access timeout for a method, <i>only</i> if the access timeout has been explicitly specified for the bean method.
     * <p/>
     * Returns an empty map if there are no explicit method level access timeout specified for the bean
     *
     * @return
     */
    public Map<EJBBusinessMethod, AccessTimeout> getMethodApplicableAccessTimeouts() {
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


}
