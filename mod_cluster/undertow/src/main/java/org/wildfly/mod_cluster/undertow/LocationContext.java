/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.listeners.HttpSessionListener;
import org.jboss.modcluster.container.listeners.ServletRequestListener;

/**
 * A simulated context, for use by Jakarta Enterprise Beans/HTTP and static locations.
 *
 * @author Stuart Douglas
 * @author Radoslav Husar
 */
public class LocationContext implements Context {

    private String contextPath;
    private UndertowHost host;

    public LocationContext(String contextPath, UndertowHost host) {
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
        return !this.host.isSuspended();
    }

    @Override
    public void addRequestListener(ServletRequestListener requestListener) {

    }

    @Override
    public void removeRequestListener(ServletRequestListener requestListener) {

    }

    @Override
    public void addSessionListener(HttpSessionListener sessionListener) {

    }

    @Override
    public void removeSessionListener(HttpSessionListener sessionListener) {

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
