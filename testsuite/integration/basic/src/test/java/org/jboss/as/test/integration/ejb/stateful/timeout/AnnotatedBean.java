/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;

/**
 * stateful session bean
 */
@Stateful
@StatefulTimeout(value = 1000, unit = TimeUnit.MILLISECONDS)
public class AnnotatedBean {
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
