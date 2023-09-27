/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;

/**
 * Lifecycle listener that cascades service removal to a series of services.
 * @author Paul Ferraro
 */
public class CascadeRemovalLifecycleListener implements LifecycleListener {

    private final Iterable<ServiceController<?>> controllers;

    public CascadeRemovalLifecycleListener(ServiceController<?> controller) {
        this.controllers = Collections.singleton(controller);
    }

    public CascadeRemovalLifecycleListener(ServiceController<?>... controllers) {
        this.controllers = Arrays.asList(controllers);
    }

    public CascadeRemovalLifecycleListener(Iterable<ServiceController<?>> controllers) {
        this.controllers = controllers;
    }

    @Override
    public void handleEvent(ServiceController<?> source, LifecycleEvent event) {
        if (event == LifecycleEvent.REMOVED) {
            for (ServiceController<?> controller : this.controllers) {
                controller.setMode(ServiceController.Mode.REMOVE);
            }
            source.removeListener(this);
        }
    }
}
