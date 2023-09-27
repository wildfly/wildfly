/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.export;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class AlphaExtension implements Extension {

    public void registerBravo(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        event.addAnnotatedType(manager.createAnnotatedType(BravoBean.class), BravoBean.class.getName());
        event.addAnnotatedType(manager.createAnnotatedType(CharlieBean.class), CharlieBean.class.getName());
    }
}
