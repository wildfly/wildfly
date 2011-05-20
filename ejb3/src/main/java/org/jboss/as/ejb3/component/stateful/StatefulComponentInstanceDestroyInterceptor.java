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

import org.jboss.as.ejb3.component.AbstractEJBInterceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.NoSuchEJBException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Destroys a SFSB instance
 *
 * @author Stuart Douglas
 */
public class StatefulComponentInstanceDestroyInterceptor extends AbstractEJBInterceptor {
    private static final Logger log = Logger.getLogger(StatefulComponentInstanceDestroyInterceptor.class);

    private final AtomicReference<Serializable> sessionIdReference;

    public StatefulComponentInstanceDestroyInterceptor(AtomicReference<Serializable> sessionIdReference) {
        this.sessionIdReference = sessionIdReference;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        // TODO: this is a contract with the client interceptor
        Serializable sessionId = this.sessionIdReference.get();
        if (sessionId == null) {
            throw new IllegalStateException("Session id hasn't been set for stateful component: " + component.getComponentName());
        }
        log.debug("Looking for stateful component instance with session id: " + sessionId);
        try {
            StatefulSessionComponentInstance instance = component.getCache().get(sessionId);
        } catch (NoSuchEJBException nsee) {
            // probably already destroyed (for example if some method had a @Remove on it)
            log.info("Could not find stateful session bean instance with id: " + sessionId + " for bean: " +
                    component.getComponentName() + " during destruction. Probably already removed");
            return null;
        }
        component.getCache().remove(sessionId);
        return null;
    }
}
