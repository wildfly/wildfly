/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

/**
 * @author Brian Stansberry
 *
 */
public class SessionSpecListenerAttribute implements HttpSessionBindingListener, HttpSessionActivationListener, Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        BOUND, UNBOUND, ACTIVATING, PASSIVATED
    };

    public static final List<Type> invocations = new ArrayList<Type>();

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.HttpSessionBindingEvent)
     */
    @Override
    public void valueBound(HttpSessionBindingEvent arg0) {
        getInvocations().add(Type.BOUND);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)
     */
    @Override
    public void valueUnbound(HttpSessionBindingEvent arg0) {
        getInvocations().add(Type.UNBOUND);
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent arg0) {
        invocations.add(Type.ACTIVATING);
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent arg0) {
        invocations.add(Type.PASSIVATED);
    }

    private List<Type> getInvocations() {
        // if (invocations == null)
        // {
        // invocations = new ArrayList<Type>();
        // }
        return invocations;
    }
}
