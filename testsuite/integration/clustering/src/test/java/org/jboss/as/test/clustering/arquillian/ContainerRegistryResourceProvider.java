/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.arquillian;

import java.lang.annotation.Annotation;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * Resource provider that exposes {@link ContainerRegistry} injection for test classes which is especially useful for manual
 * mode clustering tests.
 *
 * @author Radoslav Husar
 */
public class ContainerRegistryResourceProvider implements ResourceProvider {

    @Inject
    private Instance<ContainerRegistry> containerRegistryInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(ContainerRegistry.class);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return this.containerRegistryInstance.get();
    }
}
