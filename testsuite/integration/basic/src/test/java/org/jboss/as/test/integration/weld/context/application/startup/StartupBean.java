/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.startup;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.transaction.UserTransaction;

@ApplicationScoped
public class StartupBean {

    Startup observedPayload;

    @Resource
    UserTransaction ut;

    public void startup(@Observes Startup startup) {
        // This observer forces early bean creation which triggers lookup for UserTransaction
        this.observedPayload = startup;
    }

    public UserTransaction getTransaction() {
        return ut;
    }

    public Startup getObservedPayload() {
        return observedPayload;
    }
}
