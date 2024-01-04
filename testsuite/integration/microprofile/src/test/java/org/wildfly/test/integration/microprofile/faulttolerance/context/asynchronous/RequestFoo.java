/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.context.asynchronous;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;

/**
 * Adapted from Thorntail.
 *
 * @author Radoslav Husar
 */
@RequestScoped
public class RequestFoo {

    static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

    private String foo;

    @PostConstruct
    void init() {
        foo = "ok";
    }

    public String getFoo() {
        return foo;
    }

    @PreDestroy
    void destroy() {
        DESTROYED.set(true);
    }

}