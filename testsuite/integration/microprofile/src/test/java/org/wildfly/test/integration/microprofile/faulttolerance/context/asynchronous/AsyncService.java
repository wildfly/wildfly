/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.context.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

/**
 * Adapted from Thorntail.
 *
 * @author Radoslav Husar
 */
@ApplicationScoped
public class AsyncService {

    @Inject
    RequestFoo foo;

    @Asynchronous
    public Future<String> perform() {
        return CompletableFuture.completedFuture(foo.getFoo());
    }

}