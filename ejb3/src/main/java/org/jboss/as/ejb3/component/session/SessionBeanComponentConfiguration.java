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
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentConfiguration;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.concurrency.ContainerManagedConcurrencyInterceptor;
import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import java.util.Collections;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public abstract class SessionBeanComponentConfiguration extends EJBComponentConfiguration {

    private LockType beanLevelLockType;

    private AccessTimeout beanLevelAccessTimeout;

    private Map<EJBBusinessMethod, LockType> methodLevelLockTypes;

    private Map<EJBBusinessMethod, AccessTimeout> methodAccessTimeouts;


    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    public SessionBeanComponentConfiguration(final SessionBeanComponentDescription description) {
        super(description);

        if (description.allowsConcurrentAccess()) {
            this.beanLevelLockType = description.getBeanLevelLockType();
            this.beanLevelAccessTimeout = description.getBeanLevelAccessTimeout();

            this.methodLevelLockTypes = description.getMethodApplicableLockTypes();
            this.methodAccessTimeouts = description.getMethodApplicableAccessTimeouts();

            // container managed concurrency interceptor
            // TODO: Ideally, this should be a per bean instance (a.k.a ComponentInstance) interceptor.
            // Once, we have support for per ComponentInstance interceptors, setup this interceptor per ComponentInstance.
            if (description.getConcurrencyManagementType() != ConcurrencyManagementType.BEAN) {
                this.addComponentSystemInterceptorFactory(new ComponentInterceptorFactory() {
                    @Override
                    protected Interceptor create(Component component, InterceptorFactoryContext context) {
                        if (component instanceof LockableComponent) {
                            return new ContainerManagedConcurrencyInterceptor((LockableComponent) component);
                        } else {
                            // TODO: This shouldn't be required
                            return new Interceptor() {
                                @Override
                                public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
                                    return interceptorContext.proceed();
                                }

                            };
                        }


                    }
                });
            }

        }
    }

    @Override
    protected void addCurrentInvocationContextInterceptorFactory() {
        addComponentSystemInterceptorFactory(new ImmediateInterceptorFactory(SessionInvocationContextInterceptor.INSTANCE));
    }

    public LockType getBeanLevelLockType() {
        return this.beanLevelLockType;
    }

    public AccessTimeout getBeanLevelAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    /**
     * Returns a map of lock types applicable for the bean methods. The returned map will contain the
     * lock type for a method, <i>only</i> if the lock type has been explicitly specified for the bean method.
     * <p/>
     * Returns an empty map if there are no explicit method level lock types specified for the bean
     *
     * @return
     */
    public Map<EJBBusinessMethod, LockType> getMethodApplicableLockTypes() {
        return this.methodLevelLockTypes;
    }

    /**
     * Returns a map of {@link javax.ejb.AccessTimeout} applicable for the bean methods. The returned map will contain the
     * access timeout for a method, <i>only</i> if the access timeout has been explicitly specified for the bean method.
     * <p/>
     * Returns an empty map if there are no explicit method level access timeout specified for the bean
     *
     * @return
     */
    public Map<EJBBusinessMethod, AccessTimeout> getMethodApplicableAccessTimeouts() {
        return this.methodAccessTimeouts;
    }

    private boolean isContainerManagedConcurrency(SingletonComponentDescription singletonComponentDescription) {
        ConcurrencyManagementType concurrencyMgmtType = singletonComponentDescription.getConcurrencyManagementType();
        if (concurrencyMgmtType == ConcurrencyManagementType.BEAN) {
            return false;
        }
        return true;
    }


}
