/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

public class LegacyExtension implements Extension {

    public void observeBeforeBeanDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
        // Do nothing
    }

}
