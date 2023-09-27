/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateful;

/**
 * stateful session bean that does not configure stateful timeout via annotation or deployment descriptor.
 */
@Stateful
public class TimeoutNotConfiguredBean {
    static volatile boolean preDestroy;

    @PostConstruct
    private void postConstruct() {
        preDestroy = false;
    }

    @PreDestroy
    private void preDestroy() {
        preDestroy = true;
    }

    public void doStuff() {
    }
}
