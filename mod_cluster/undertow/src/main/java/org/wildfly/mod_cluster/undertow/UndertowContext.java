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

package org.wildfly.mod_cluster.undertow;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.core.InMemorySessionManagerFactory;
import io.undertow.servlet.core.ManagedListener;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;

/**
 * Adapts {@link Deployment} to an {@link Context}.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @since 8.0
 */
public class UndertowContext implements Context {

    private final Deployment deployment;
    private final UndertowHost host;

    public UndertowContext(Deployment deployment, UndertowHost host) {
        this.deployment = deployment;
        this.host = host;
    }

    @Override
    public Host getHost() {
        return this.host;
    }

    @Override
    public String getPath() {
        String path = this.deployment.getDeploymentInfo().getContextPath();
        return "/".equals(path) ? "" : path;
    }

    @Override
    public boolean isStarted() {
        return this.deployment.getApplicationListeners().isStarted()
                && !this.host.isSuspended();
    }

    @Override
    public void addRequestListener(org.jboss.modcluster.container.listeners.ServletRequestListener requestListener) {
        ServletRequestListener listener = new ServletRequestListener() {
            @Override
            public void requestInitialized(ServletRequestEvent sre) {
                requestListener.requestInitialized();
            }

            @Override
            public void requestDestroyed(ServletRequestEvent sre) {
                requestListener.requestDestroyed();
            }
        };
        ManagedListener ml = new ManagedListener(new ListenerInfo(ServletRequestListener.class, new ImmediateInstanceFactory<>(listener)), true);
        try {
            ml.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        this.deployment.getApplicationListeners().addListener(ml);
    }

    @Override
    public void removeRequestListener(org.jboss.modcluster.container.listeners.ServletRequestListener requestListener) {
        // Do nothing
    }

    @Override
    public void addSessionListener(org.jboss.modcluster.container.listeners.HttpSessionListener sessionListener) {
        HttpSessionListener listener = new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
                sessionListener.sessionCreated();
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                sessionListener.sessionDestroyed();
            }
        };
        ManagedListener ml = new ManagedListener(new ListenerInfo(HttpSessionListener.class, new ImmediateInstanceFactory<>(listener)), true);
        try {
            ml.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        this.deployment.getApplicationListeners().addListener(ml);
    }

    @Override
    public void removeSessionListener(org.jboss.modcluster.container.listeners.HttpSessionListener sessionListener) {
        // Do nothing
    }

    @Override
    public int getActiveSessionCount() {
        return this.deployment.getSessionManager().getActiveSessions().size();
    }

    @Override
    public boolean isDistributable() {
        return !(this.deployment.getDeploymentInfo().getSessionManagerFactory() instanceof InMemorySessionManagerFactory);
    }

    @Override
    public String toString() {
        return this.getPath();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UndertowContext)) return false;

        UndertowContext context = (UndertowContext) object;
        return this.host.equals(context.host) && this.getPath().equals(context.getPath());
    }

    @Override
    public int hashCode() {
        return this.host.hashCode() ^ this.getPath().hashCode();
    }
}
