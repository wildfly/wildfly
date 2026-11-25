/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.service;

import org.wildfly.clustering.server.service.Service;
import org.wildfly.service.BlockingLifecycle;

/**
 * A blocking lifecycle instrumentation of a service.
 * @author Paul Ferraro
 */
public class ServiceLifecycle implements BlockingLifecycle {
    private final Service service;

    public ServiceLifecycle(Service service) {
        this.service = service;
    }

    @Override
    public boolean isStarted() {
        return this.service.isStarted();
    }

    @Override
    public void start() {
        this.service.start();
    }

    @Override
    public void stop() {
        this.service.stop();
    }
}
