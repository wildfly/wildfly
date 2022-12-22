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
