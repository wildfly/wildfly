/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.kohsuke.MetaInfServices;

/**
 * Arquillian loadable extension which registers {@link ContainerRegistryResourceProvider}.
 *
 * @author Radoslav Husar
 */
@MetaInfServices(LoadableExtension.class)
public class ContainerRegistryResourceProviderLoadableExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ResourceProvider.class, ContainerRegistryResourceProvider.class);
    }
}
