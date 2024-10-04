/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.concurrency.adapter;

import jakarta.enterprise.concurrent.ContextService;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The adapter for {@link ContextService}.
 * @author Eduardo Martins
 */
public class ContextServiceAdapter implements ContextService {

    private final ContextService contextService;
    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;

    /**
     *
     * @param contextService
     * @param contextServiceTypesConfiguration
     */
    public ContextServiceAdapter(ContextService contextService, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        this.contextService = contextService;
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration() {
        return contextServiceTypesConfiguration;
    }

    @Override
    public <R> Callable<R> contextualCallable(Callable<R> callable) {
        return contextService.contextualCallable(callable);
    }

    @Override
    public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> biConsumer) {
        return contextService.contextualConsumer(biConsumer);
    }

    @Override
    public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        return contextService.contextualConsumer(consumer);
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> biFunction) {
        return contextService.contextualFunction(biFunction);
    }

    @Override
    public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        return contextService.contextualFunction(function);
    }

    @Override
    public Runnable contextualRunnable(Runnable runnable) {
        return contextService.contextualRunnable(runnable);
    }

    @Override
    public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        return contextService.contextualSupplier(supplier);
    }

    @Override
    public <T> T createContextualProxy(T t, Class<T> aClass) {
        return contextService.createContextualProxy(t, aClass);
    }

    @Override
    public Object createContextualProxy(Object o, Class<?>... classes) {
        return contextService.createContextualProxy(o, classes);
    }

    @Override
    public <T> T createContextualProxy(T t, Map<String, String> map, Class<T> aClass) {
        return contextService.createContextualProxy(t, map, aClass);
    }

    @Override
    public Object createContextualProxy(Object o, Map<String, String> map, Class<?>... classes) {
        return contextService.createContextualProxy(o, map, classes);
    }

    @Override
    public Executor currentContextExecutor() {
        return contextService.currentContextExecutor();
    }

    @Override
    public Map<String, String> getExecutionProperties(Object o) {
        return contextService.getExecutionProperties(o);
    }

    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> completableFuture) {
        return contextService.withContextCapture(completableFuture);
    }

    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> completionStage) {
        return contextService.withContextCapture(completionStage);
    }
}
