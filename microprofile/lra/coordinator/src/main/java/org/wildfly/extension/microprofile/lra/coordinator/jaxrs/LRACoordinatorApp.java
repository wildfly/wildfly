/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator.jaxrs;

import io.narayana.lra.coordinator.api.Coordinator;
import io.narayana.lra.coordinator.api.CoordinatorContainerFilter;

import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class LRACoordinatorApp extends Application {
    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    public LRACoordinatorApp() {
        classes.add(Coordinator.class);
        singletons.add(new CoordinatorContainerFilter());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}