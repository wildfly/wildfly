/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.Deployment;

/**
 * Server/deployment lifecycle event listener.
 * <p/>
 * TODO: implement commented out Undertow events
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 */
public interface UndertowEventListener {
    default void onShutdown() {
    }

    //void onDeploymentAdd(DeploymentInfo deploymentInfo, Host host);

    default void onDeploymentStart(Deployment deployment, Host host) {
    }

    default void onDeploymentStop(Deployment deployment, Host host) {
    }

    default void onDeploymentStart(String contextPath, Host host) {

    }

    default void onDeploymentStop(String contextPath, Host host) {

    }

    //void onDeploymentRemove(DeploymentInfo deploymentInfo, Host host);

    //void onHostAdd(Host host);

    //void onHostRemove(Host host);

    default void onHostStart(Host host) {
    }

    default void onHostStop(Host host) {
    }

    //void onServerAdd(Server server);

    //void onServerRemove(Server server);

    default void onServerStart(Server server) {
    }

    default void onServerStop(Server server) {
    }
}
