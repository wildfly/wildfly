/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.faulttolerance.context.timeout;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * @author Radoslav Husar
 */
public class TimeoutBean {

    @Inject
    RequestScopedService service;

    @Timeout
    public String greet() throws InterruptedException {
        return "Hello " + service.call();
    }
}
