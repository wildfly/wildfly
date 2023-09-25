/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;


import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import jakarta.ejb.EJBLocalObject;
import jakarta.ejb.EJBObject;
import jakarta.ejb.TransactionAttributeType;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.invocation.InterceptorContext;

import static java.util.Collections.emptyMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponent extends EJBComponent {

    protected final Map<String, AccessTimeoutDetails> beanLevelAccessTimeout;
    private final ExecutorService asyncExecutor;

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
    }

    public <T> T getBusinessObject(Class<T> businessInterface, final InterceptorContext context) throws IllegalStateException {
        if (businessInterface == null) {
            throw EjbLogger.ROOT_LOGGER.businessInterfaceIsNull();
        }
        return createViewInstanceProxy(businessInterface, emptyMap());
    }

    public EJBLocalObject getEJBLocalObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbLocalObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.beanComponentMissingEjbObject(getComponentName(), "EJBLocalObject");
        }
        return createViewInstanceProxy(EJBLocalObject.class, Collections.<Object, Object>emptyMap(), getEjbLocalObjectViewServiceName());
    }

    public EJBObject getEJBObject(final InterceptorContext ctx) throws IllegalStateException {
        if (getEjbObjectViewServiceName() == null) {
            throw EjbLogger.ROOT_LOGGER.beanComponentMissingEjbObject(getComponentName(), "EJBObject");
        }
        return createViewInstanceProxy(EJBObject.class, Collections.<Object, Object>emptyMap(), getEjbObjectViewServiceName());
    }

    /**
     * Return the {@link java.util.concurrent.Executor} used for asynchronous invocations.
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
            throw EjbLogger.ROOT_LOGGER.getRollBackOnlyIsNotAllowWithSupportsAttribute();
        }
        return super.getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        // NOT_SUPPORTED and NEVER will not have a transaction context, so we can ignore those
        if (getCurrentTransactionAttribute() == TransactionAttributeType.SUPPORTS) {
            throw EjbLogger.ROOT_LOGGER.setRollbackOnlyNotAllowedForSupportsTxAttr();
        }
        super.setRollbackOnly();
    }

}
