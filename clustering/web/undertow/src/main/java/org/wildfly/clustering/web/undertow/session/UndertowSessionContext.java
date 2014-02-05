/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.session;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.core.ApplicationListeners;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.wildfly.clustering.web.session.SessionContext;

/**
 * {@link SessionContext} that delegate to a {@link Deployment}.
 * @author Paul Ferraro
 */
public class UndertowSessionContext implements SessionContext {

    private final Deployment deployment;

    public UndertowSessionContext(Deployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public Iterable<HttpSessionListener> getSessionListeners() {
        final ApplicationListeners listeners = this.deployment.getApplicationListeners();
        HttpSessionListener listener = new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent event) {
                listeners.sessionCreated(event.getSession());
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent event) {
                listeners.sessionDestroyed(event.getSession());
            }
        };
        return Collections.singleton(listener);
    }

    @Override
    public Iterable<HttpSessionAttributeListener> getSessionAttributeListeners() {
        final ApplicationListeners listeners = this.deployment.getApplicationListeners();
        HttpSessionAttributeListener listener = new HttpSessionAttributeListener() {
            @Override
            public void attributeAdded(HttpSessionBindingEvent event) {
                listeners.httpSessionAttributeAdded(event.getSession(), event.getName(), event.getValue());
            }

            @Override
            public void attributeRemoved(HttpSessionBindingEvent event) {
                listeners.httpSessionAttributeRemoved(event.getSession(), event.getName(), event.getValue());
            }

            @Override
            public void attributeReplaced(HttpSessionBindingEvent event) {
                listeners.httpSessionAttributeReplaced(event.getSession(), event.getName(), event.getValue());
            }
        };
        return Collections.singleton(listener);
    }

    @Override
    public ServletContext getServletContext() {
        return this.deployment.getServletContext();
    }
}
