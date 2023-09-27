/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mgmt.access;

import org.jboss.arquillian.core.spi.LoadableExtension;


/**
 * Custom arquillian extension to allow us register notifications about server lifecycle.
 */
public class CustomLoadableExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(ServerLifecycleObserver.class);
    }
}
