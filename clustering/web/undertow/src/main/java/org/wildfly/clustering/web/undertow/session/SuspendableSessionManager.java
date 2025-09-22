/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.concurrent.Executor;

import org.jboss.as.clustering.service.SuspendableService;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;

/**
 * A session manager decorator that restarts itself on suspend/resume.
 * @author Paul Ferraro
 */
public class SuspendableSessionManager extends DecoratedSessionManager {

    public SuspendableSessionManager(UndertowSessionManager manager, SuspendableActivityRegistry registry, Executor executor) {
        super(manager, new SuspendableService(manager, registry, executor));
    }
}
