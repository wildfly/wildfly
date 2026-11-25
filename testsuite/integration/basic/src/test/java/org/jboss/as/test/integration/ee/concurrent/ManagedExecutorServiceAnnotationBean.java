/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;

@ManagedExecutorDefinition(
        name = "java:module/concurrent/MyExecutor",
        context = "java:module/concurrent/MyExecutorContext",
        hungTaskThreshold = 120000,
        maxAsync = 5)
@ContextServiceDefinition(
        name = "java:module/concurrent/MyExecutorContext",
        propagated = { SECURITY, APPLICATION })
@Stateless
@LocalBean
public class ManagedExecutorServiceAnnotationBean {

    @Resource(lookup = "java:module/concurrent/MyExecutor")
    ManagedExecutorService executorService;

    public Future<?> testRunnable(Runnable runnable) throws ExecutionException, InterruptedException {
        assert executorService != null;
        return executorService.submit(runnable);
    }
}
