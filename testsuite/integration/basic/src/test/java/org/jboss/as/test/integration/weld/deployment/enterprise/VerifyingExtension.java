/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

public class VerifyingExtension implements Extension {

    private List<Class<?>> observedAnnotatedTypes = new ArrayList<Class<?>>();

    public void observeProcessAnnotatedType(@Observes ProcessAnnotatedType<?> event) {
        observedAnnotatedTypes.add(event.getAnnotatedType().getJavaClass());
    }

    public List<Class<?>> getObservedAnnotatedTypes() {
        return observedAnnotatedTypes;
    }

}
