/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.faulttolerance.context.timeout;

import jakarta.enterprise.context.RequestScoped;

/**
 * @author Radoslav Husar
 */
@RequestScoped
public class RequestScopedService {

    public String call() {
        return "bar";
    }
}
