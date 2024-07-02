/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.arquillian;

import java.lang.annotation.Annotation;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.as.arquillian.container.AbstractTargetsContainerProvider;
import org.testcontainers.containers.GenericContainer;

public class TestContainerProvider extends AbstractTargetsContainerProvider {
    @Inject
    @ClassScoped
    private Instance<GenericContainer<?>> genericContainerInstance;

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        return genericContainerInstance.get();
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return GenericContainer.class.isAssignableFrom(type);
    }
}
