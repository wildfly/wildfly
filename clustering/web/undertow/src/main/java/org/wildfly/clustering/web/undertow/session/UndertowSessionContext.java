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

import java.util.Collection;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.wildfly.clustering.web.session.SessionContext;

import io.undertow.servlet.api.Deployment;

/**
 * {@link SessionContext} that delegate to a {@link Deployment}.
 * @author Paul Ferraro
 */
public class UndertowSessionContext implements SessionContext, HttpSessionListener, HttpSessionAttributeListener {

    private final Deployment deployment;

    public UndertowSessionContext(Deployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public Collection<HttpSessionListener> getSessionListeners() {
        return Collections.singleton(this);
    }

    @Override
    public Collection<HttpSessionAttributeListener> getSessionAttributeListeners() {
        return Collections.singleton(this);
    }

    @Override
    public ServletContext getServletContext() {
        return this.deployment.getServletContext();
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        this.deployment.getApplicationListeners().httpSessionAttributeAdded(event.getSession(), event.getName(), event.getValue());
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        this.deployment.getApplicationListeners().httpSessionAttributeRemoved(event.getSession(), event.getName(), event.getValue());
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        this.deployment.getApplicationListeners().httpSessionAttributeReplaced(event.getSession(), event.getName(), event.getValue());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        this.deployment.getApplicationListeners().sessionCreated(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        this.deployment.getApplicationListeners().sessionDestroyed(event.getSession());
    }
}
