/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Associate the proper component instance to the invocation based on the passed in session identifier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulComponentInstanceInterceptor extends AbstractEJBInterceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new StatefulComponentInstanceInterceptor());
    private static final Object STATEFUL_BEAN_KEY = StatefulSessionBean.class;

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        // TODO: this is a contract with the client interceptor
        SessionID sessionId = context.getPrivateData(SessionID.class);
        if (sessionId == null) {
            throw EjbLogger.ROOT_LOGGER.statefulSessionIdIsNull(component.getComponentName());
        }
        TransactionSynchronizationRegistry tsr = component.getTransactionSynchronizationRegistry();
        // Use bean associated with current tx, if one exists
        @SuppressWarnings({ "resource", "unchecked" })
        StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean = (tsr.getTransactionKey() != null) ? (StatefulSessionBean<SessionID, StatefulSessionComponentInstance>) tsr.getResource(sessionId) : null;
        if (bean == null) {
            ROOT_LOGGER.debugf("Looking for stateful component instance with session id: %s", sessionId);
            bean = component.getCache().findStatefulSessionBean(sessionId);
        }
        if (bean == null) {
            //This exception will be transformed into the correct exception type by the exception transforming interceptor
            throw EjbLogger.ROOT_LOGGER.couldNotFindEjb(sessionId.toString());
        }
        try {
            context.putPrivateData(StatefulSessionBean.class, bean);
            context.putPrivateData(ComponentInstance.class, bean.getInstance());
            return context.proceed();
        } catch (Exception ex) {
            if (component.shouldDiscard(ex, context.getMethod())) {
                ROOT_LOGGER.tracef(ex, "Removing bean %s because of exception", sessionId);
                bean.discard();
            }
            throw ex;
        } catch (Error e) {
            ROOT_LOGGER.tracef(e, "Removing bean %s because of error", sessionId);
            bean.discard();
            throw e;
        } catch (Throwable t) {
            ROOT_LOGGER.tracef(t, "Removing bean %s because of Throwable", sessionId);
            bean.discard();
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    static StatefulSessionBean<SessionID, StatefulSessionComponentInstance> getBean(InterceptorContext context) {
        return (StatefulSessionBean<SessionID, StatefulSessionComponentInstance>) context.getPrivateData(STATEFUL_BEAN_KEY);
    }
}
