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

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.core.InMemorySessionManagerFactory;
import io.undertow.servlet.core.ManagedListener;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

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

    private Deployment deployment;
    private Host host;

    public UndertowContext(Deployment deployment, Host host) {
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
        return this.deployment.getApplicationListeners().isStarted();
    }

    @Override
    public void addRequestListener(ServletRequestListener listener) {
        ManagedListener ml = new ManagedListener(new ListenerInfo(ServletRequestListener.class, new ImmediateInstanceFactory<>(listener)), true);
        try {
            ml.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        this.deployment.getApplicationListeners().addListener(ml);
    }

    @Override
    public void removeRequestListener(ServletRequestListener listener) {
        // Do nothing
    }

    @Override
    public void addSessionListener(HttpSessionListener listener) {
        ManagedListener ml = new ManagedListener(new ListenerInfo(HttpSessionListener.class, new ImmediateInstanceFactory<>(listener)), true);
        try {
            ml.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        this.deployment.getApplicationListeners().addListener(ml);
    }

    @Override
    public void removeSessionListener(HttpSessionListener listener) {
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
