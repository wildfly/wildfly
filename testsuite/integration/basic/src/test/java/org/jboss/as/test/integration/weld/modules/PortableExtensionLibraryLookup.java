/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PortableExtensionLibraryLookup implements PortableExtensionLookup {

    @Inject
    private PortableExtension portableExtension;

    @Override
    public PortableExtension getPortableExtension() {
        return portableExtension;
    }
}
