/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

@ApplicationScoped
public class PortableExtension implements Extension {

    private BeanManager beanManager;

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event, final BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public BeanManager getBeanManager() {
        return this.beanManager;
    }
}
