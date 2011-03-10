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
package org.jboss.as.ejb3.component.session.stateful;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Associate the proper component instance to the invocation based on the passed in session identifier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ComponentInstanceInterceptor implements Interceptor {
    private static final Logger log = Logger.getLogger(ComponentInstanceInterceptor.class);

    protected static <C extends Component> C getComponent(InterceptorContext context, Class<C> componentType) {
        Component component = context.getPrivateData(Component.class);
        if (component == null) {
            throw new IllegalStateException("Component not set in InterceptorContext: " + context);
        }
        return componentType.cast(component);
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        // TODO: this is a contract with the client interceptor
        Serializable sessionId = context.getPrivateData(Serializable.class);
        StatefulSessionComponentInstance instance = component.getCache().get(sessionId);
        try {
            context.putPrivateData(ComponentInstance.class, instance);
            return context.proceed();
        } catch (Exception ex) {
            // TODO: detect app exception
            //if (StatefulRemoveInterceptor.isApplicationException(ex, (MethodInvocation)invocation)) throw ex;
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                if (log.isTraceEnabled())
                    log.trace("Removing bean " + sessionId + " because of exception", ex);
                component.getCache().discard(sessionId);
            }
            throw ex;
        } catch (final Error e) {
            if (log.isTraceEnabled())
                log.trace("Removing bean " + sessionId + " because of error", e);
            component.getCache().discard(sessionId);
            throw e;
        } catch (final Throwable t) {
            if (log.isTraceEnabled())
                log.trace("Removing bean " + sessionId + " because of Throwable", t);
            component.getCache().discard(sessionId);
            throw new RuntimeException(t);
        } finally {
            // nobody attached the bean to the tx and nobody discarded it
            //if (!target.isTxSynchronized() && !target.isDiscarded()) container.getCache().release(target);
            component.getCache().release(instance);
            // TODO: remove instance from context
        }
    }
}
