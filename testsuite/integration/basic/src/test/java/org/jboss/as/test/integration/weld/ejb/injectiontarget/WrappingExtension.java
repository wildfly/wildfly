/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injectiontarget;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;

public class WrappingExtension implements Extension {

    public void wrapInjectionTarget(@Observes ProcessInjectionTarget<Bus> event) {
        final InjectionTarget<Bus> injectionTarget = event.getInjectionTarget();
        event.setInjectionTarget(new ForwardingInjectionTarget<Bus>() {

            @Override
            public void inject(Bus instance, CreationalContext<Bus> ctx) {
                super.inject(instance, ctx);
                instance.setInitialized(true);
            }

            @Override
            public InjectionTarget<Bus> getDelegate() {
                return injectionTarget;
            }
        });
    }
}
