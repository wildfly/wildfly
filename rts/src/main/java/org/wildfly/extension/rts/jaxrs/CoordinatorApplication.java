/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.jaxrs;

import java.util.Set;

import org.jboss.jbossts.star.service.TMApplication;

import jakarta.ws.rs.core.Application;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class CoordinatorApplication extends Application {

    private final TMApplication tmApplication;

    public CoordinatorApplication() {
        tmApplication = new TMApplication();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return tmApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return tmApplication.getSingletons();
    }
}
