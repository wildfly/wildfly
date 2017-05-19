/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.mod_cluster.undertow;

import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;

/**
 * A simulated context, for use by EJB/HTTP and static locations.
 *
 * @author Stuart Douglas
 * @author Radoslav Husar
 */
public class LocationContext implements Context {

    private String contextPath;
    private Host host;

    public LocationContext(String contextPath, Host host) {
        this.contextPath = contextPath;
        this.host = host;
    }

    @Override
    public Host getHost() {
        return this.host;
    }

    @Override
    public String getPath() {
        String path = contextPath;
        return "/".equals(path) ? "" : path;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void addRequestListener(ServletRequestListener listener) {

    }

    @Override
    public void removeRequestListener(ServletRequestListener listener) {
        // Do nothing
    }

    @Override
    public void addSessionListener(HttpSessionListener listener) {

    }

    @Override
    public void removeSessionListener(HttpSessionListener listener) {
        // Do nothing
    }

    @Override
    public int getActiveSessionCount() {
        return 0;
    }

    @Override
    public boolean isDistributable() {
        return false;
    }

    @Override
    public String toString() {
        return this.getPath();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LocationContext)) return false;

        LocationContext context = (LocationContext) object;
        return this.host.equals(context.host) && this.getPath().equals(context.getPath());
    }

    @Override
    public int hashCode() {
        return this.host.hashCode() ^ this.getPath().hashCode();
    }
}
