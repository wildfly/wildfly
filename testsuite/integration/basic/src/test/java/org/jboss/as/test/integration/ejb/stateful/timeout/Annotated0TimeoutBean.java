/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import java.util.concurrent.TimeUnit;

/**
 * stateful session bean with timeout value 0.
 */
@Stateful
@StatefulTimeout(value = 0, unit = TimeUnit.MILLISECONDS)
public class Annotated0TimeoutBean {

    public static volatile boolean preDestroy = false;

    @PreDestroy
    public void preDestroy() {
        preDestroy = true;
    }

    public void doStuff() {
    }
}
