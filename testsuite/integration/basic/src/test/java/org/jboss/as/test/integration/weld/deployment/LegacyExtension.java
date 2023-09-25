/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

public class LegacyExtension implements Extension {

    public void observeBeforeBeanDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(beanManager.createAnnotatedType(LegacyAlpha.class), LegacyExtension.class.getName());
    }

}
