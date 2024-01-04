/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.spec.basic.resource;

import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;


public class JaxrsAppTwo extends Application {
    public static Set<Class<?>> classes = new HashSet<Class<?>>();

    @Override
    public Set<Class<?>> getClasses() {
        classes.add(JaxrsAppResource.class);
        return classes;
    }
}
