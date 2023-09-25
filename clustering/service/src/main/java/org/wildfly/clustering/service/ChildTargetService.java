/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Consumer;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that performs service installation into the child target on start.
 * @author Paul Ferraro
 */
public class ChildTargetService implements Service {
    private final Consumer<ServiceTarget> installer;

    public ChildTargetService(Consumer<ServiceTarget> installer) {
        this.installer = installer;
    }

    @Override
    public void start(StartContext context) {
        this.installer.accept(context.getChildTarget());
    }

    @Override
    public void stop(StopContext context) {
        // Services installed into child target are auto-removed after this service stops.
    }
}
