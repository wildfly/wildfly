/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import io.undertow.servlet.api.Deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.suspend.SuspendController;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;

/**
 * Adapts {@link org.wildfly.extension.undertow.Host} to an {@link Host}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class UndertowHost implements Host {

    private org.wildfly.extension.undertow.Host host;
    private Engine engine;

    public UndertowHost(org.wildfly.extension.undertow.Host host, Engine engine) {
        this.engine = engine;
        this.host = host;
    }

    @Override
    public String getName() {
        return this.host.getName();
    }

    @Override
    public Engine getEngine() {
        return this.engine;
    }

    @Override
    public Iterable<Context> getContexts() {
        List<Context> contexts = new ArrayList<>();
        for (Deployment deployment : this.host.getDeployments()) {
            contexts.add(new UndertowContext(deployment, this));
        }
        for (String path : this.host.getLocations()) {
            contexts.add(new LocationContext(path, this));
        }
        return contexts;
    }

    @Override
    public Set<String> getAliases() {
        return this.host.getAllAliases();
    }

    @Override
    public Context findContext(String path) {
        String findPath = "".equals(path) ? "/" : path;

        for (Deployment deployment : this.host.getDeployments()) {
            if (deployment.getDeploymentInfo().getContextPath().equals(findPath)) {
                return new UndertowContext(deployment, this);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UndertowHost)) return false;

        UndertowHost host = (UndertowHost) object;
        return this.getName().equals(host.getName());
    }

    @Override
    public int hashCode() {
        return this.host.getName().hashCode();
    }

    @Override
    public String toString() {
        return this.host.getName();
    }

    boolean isSuspended() {
        return this.host.getSuspendState() == SuspendController.State.SUSPENDED;
    }
}
