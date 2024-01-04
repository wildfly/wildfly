/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.configuration.nonportablemode;

import jakarta.enterprise.event.Observes;import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;


public class NonPortableExtension implements Extension {

    void beforeDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        manager.getBeans("foo"); // this call is not allowed in the BeforeBeanDiscovery phase
    }

    void beansDiscovered(@Observes AfterBeanDiscovery event, BeanManager manager) {
        manager.getBeans("foo"); // this call is not allowed in the AfterBeanDiscovery phase
    }
}
