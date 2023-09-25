/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
