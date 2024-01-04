/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class PortableExtensionSubdeploymentLookup implements PortableExtensionLookup {

    @Inject
    private PortableExtension portableExtension;

    @Override
    public PortableExtension getPortableExtension() {
        return portableExtension;
    }
}
