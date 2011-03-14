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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.session.SessionBeanComponentConfiguration;
import org.jboss.as.ejb3.concurrency.ContainerManagedConcurrencyInterceptor;
import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public class SingletonComponentConfiguration extends SessionBeanComponentConfiguration {

    private boolean initOnStartup;

    private LockType beanLevelLockType;

    private Map<EJBBusinessMethod, LockType> methodLevelLockTypes;

    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    public SingletonComponentConfiguration(final SingletonComponentDescription description) {
        super(description);

        this.initOnStartup = description.isInitOnStartup();
        this.beanLevelLockType = description.getBeanLevelLockType();
        this.methodLevelLockTypes = description.getMethodApplicableLockTypes();

        // instance associating interceptor
        this.addComponentSystemInterceptorFactory(new ImmediateInterceptorFactory(new SingletonComponentInstanceAssociationInterceptor()));
        // container managed concurrency interceptor
        if (description.getConcurrencyManagementType() != ConcurrencyManagementType.BEAN) {
            this.addComponentSystemInterceptorFactory(new ComponentInterceptorFactory() {
                @Override
                protected Interceptor create(Component component, InterceptorFactoryContext context) {
                    return new ContainerManagedConcurrencyInterceptor((LockableComponent) component);
                }
            });
        }
    }

    @Override
    public AbstractComponent constructComponent() {
        return new SingletonComponent(this);
    }

    public boolean isInitOnStartup() {
        return this.initOnStartup;
    }

    public LockType getBeanLevelLockType() {
        return this.beanLevelLockType;
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

    private boolean isContainerManagedConcurrency(SingletonComponentDescription singletonComponentDescription) {
        ConcurrencyManagementType concurrencyMgmtType = singletonComponentDescription.getConcurrencyManagementType();
        if (concurrencyMgmtType == ConcurrencyManagementType.BEAN) {
            return false;
        }
        return true;
    }
}
