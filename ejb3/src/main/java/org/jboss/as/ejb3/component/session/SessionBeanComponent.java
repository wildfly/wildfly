/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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


import static java.util.Collections.emptyMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TransactionAttributeType;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponent extends EJBComponent {

    protected final Map<String, AccessTimeoutDetails> beanLevelAccessTimeout;
    private final ExecutorService asyncExecutor;

    private final ServiceName ejbObjectView;
    private final ServiceName ejbLocalObjectView;

    /**
     * Construct a new instance.
     *
     * @param ejbComponentCreateService the component configuration
     */
    protected SessionBeanComponent(final SessionBeanComponentCreateService ejbComponentCreateService) {
        super(ejbComponentCreateService);

        this.beanLevelAccessTimeout = ejbComponentCreateService.getBeanAccessTimeout();
        //ejbComponentCreateService.getAsynchronousMethods();
        //        this.asyncExecutor = (Executor) ejbComponentCreateService.getInjection(ASYNC_EXECUTOR_SERVICE_NAME).getValue();

        //if this bean has no async methods, then this will not be injected
        this.asyncExecutor = ejbComponentCreateService.getAsyncExecutorService().getOptionalValue();
        this.ejbLocalObjectView = ejbComponentCreateService.getEjbLocalObjectView();
        this.ejbObjectView = ejbComponentCreateService.getEjbObjectview();
    }

    public <T> T getBusinessObject(Class<T> businessInterface, final InterceptorContext context) throws IllegalStateException {
        if (businessInterface == null) {
            throw MESSAGES.businessInterfaceIsNull();
        }
        return createViewInstanceProxy(businessInterface, emptyMap());
    }

    public EJBLocalObject getEJBLocalObject(final InterceptorContext ctx) throws IllegalStateException {
        if (ejbLocalObjectView == null) {
            throw MESSAGES.beanComponentMissingEjbObject(getComponentName(),"EJBLocalObject");
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.<Object, Object>emptyMap(), ejbLocalObjectView);
    }

    public EJBObject getEJBObject(final InterceptorContext ctx) throws IllegalStateException {
        if (ejbObjectView == null) {
            throw MESSAGES.beanComponentMissingEjbObject(getComponentName(),"EJBObject");
        }
        return createViewInstanceProxy(EJBObject.class, Collections.<Object, Object>emptyMap(), ejbObjectView);
    }

    /**
     * Return the {@link Executor} used for asynchronous invocations.
     *
     * @return the async executor
     */
    public ExecutorService getAsynchronousExecutor() {
        return asyncExecutor;
    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        // NOT_SUPPORTED and NEVER will not have a transaction context, so we can ignore those
        if (getCurrentTransactionAttribute() == TransactionAttributeType.SUPPORTS) {
            throw MESSAGES.getRollBackOnlyIsNotAllowWithSupportsAttribute();
        }
        return super.getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        // NOT_SUPPORTED and NEVER will not have a transaction context, so we can ignore those
        if (getCurrentTransactionAttribute() == TransactionAttributeType.SUPPORTS) {
            throw new IllegalStateException("EJB 3.1 FR 13.6.2.8 setRollbackOnly is not allowed with SUPPORTS attribute");
        }
        super.setRollbackOnly();
    }

    protected ServiceName getEjbObjectView() {
        return ejbObjectView;
    }

    protected ServiceName getEjbLocalObjectView() {
        return ejbLocalObjectView;
    }
}
