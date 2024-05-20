/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class TestContainersExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        builder
                .observer(TestContainersObserver.class)
                .service(ResourceProvider.class, TestContainerProvider.class);
    }
}
