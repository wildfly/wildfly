/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import java.time.Duration;

import org.jboss.as.server.suspend.SuspendController;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Encapsulates the configuration of an {@link UndertowEventHandlerAdapterService}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface UndertowEventHandlerAdapterConfiguration {
    Duration getStatusInterval();
    UndertowService getUndertowService();
    ContainerEventHandler getContainerEventHandler();
    SuspendController getSuspendController();
    UndertowListener getListener();
    Server getServer();
}
