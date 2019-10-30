/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
