/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.context.application.event;

import jakarta.ejb.Stateless;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

@Stateless
public class SessionBean {

    public static volatile Object EVENT;

    public static void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        if (EVENT != null) {
            throw new IllegalStateException("Event already received: " + EVENT);
        }
        EVENT = event;
    }

    public void ping() {
    }
}
