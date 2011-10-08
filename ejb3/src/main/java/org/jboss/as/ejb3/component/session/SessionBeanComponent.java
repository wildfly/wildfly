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
import org.jboss.as.ejb3.context.spi.SessionContext;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.ejb.client.SessionID;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponent extends EJBComponent implements org.jboss.as.ejb3.context.spi.SessionBeanComponent {

    private static final Logger logger = Logger.getLogger(SessionBeanComponent.class);

    public static final ServiceName ASYNC_EXECUTOR_SERVICE_NAME = ThreadsServices.EXECUTOR.append("ejb3-async");

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

    @Override
    public <T> T getBusinessObject(SessionContext ctx, Class<T> businessInterface) throws IllegalStateException {
        if (businessInterface == null) {
            throw new IllegalStateException("Business interface type cannot be null");
        }
        return createViewInstanceProxy(businessInterface, emptyMap());
    }

    protected SessionID getSessionIdOf(final SessionContext ctx) {
        return ((SessionBeanComponentInstance.SessionBeanComponentInstanceContext) ctx).getId();
    }

    @Override
    public EJBLocalObject getEJBLocalObject(SessionContext ctx) throws IllegalStateException {
        if (ejbLocalObjectView == null) {
            throw new IllegalStateException("Bean " + getComponentName() + " does not have an EJBLocalObject");
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.<Object, Object>singletonMap(SessionID.SESSION_ID_KEY, getSessionIdOf(ctx)), ejbLocalObjectView);
    }

    @Override
    public EJBObject getEJBObject(SessionContext ctx) throws IllegalStateException {
        if (ejbObjectView == null) {
            throw new IllegalStateException("Bean " + getComponentName() + " does not have an EJBObject");
        }
        return createViewInstanceProxy(EJBObject.class, Collections.<Object, Object>singletonMap(SessionID.SESSION_ID_KEY, getSessionIdOf(ctx)), ejbObjectView);
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
            throw new IllegalStateException("EJB 3.1 FR 13.6.2.9 getRollbackOnly is not allowed with SUPPORTS attribute");
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
}
