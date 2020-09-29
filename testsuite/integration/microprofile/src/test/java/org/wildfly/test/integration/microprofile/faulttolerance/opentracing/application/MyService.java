package org.wildfly.test.integration.microprofile.faulttolerance.opentracing.application;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class MyService {
    @Inject
    private Tracer tracer;

    private AtomicInteger counter = new AtomicInteger();

    public void reset() {
        counter.set(0);
    }

    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        try (Scope scope = tracer.buildSpan("hello").startActive(true)) {
            scope.span().log("attempt " + counter.getAndIncrement());
            throw new RuntimeException("failed");
        }
    }

    public String fallback() {
        try (Scope scope = tracer.buildSpan("fallback").startActive(true)) {
            return "Hello from fallback";
        }
    }

    @Asynchronous
    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallbackAsync")
    public CompletionStage<String> helloAsync() {
        try (Scope scope = tracer.buildSpan("helloAsync").startActive(true)) {
            scope.span().log("attempt " + counter.getAndIncrement());
            CompletableFuture<String> result = new CompletableFuture<>();
            result.completeExceptionally(new RuntimeException("failed with scope " + scope));
            scope.span().log("Current scope " + scope);
            return result;
        }
    }

    public CompletionStage<String> fallbackAsync() {
        try (Scope scope = tracer.buildSpan("fallbackAsync").startActive(true)) {
            scope.span().log("fallbackAsync " + counter.get());
            scope.span().log("Current scope " + scope);
            return CompletableFuture.completedFuture("Hello from async fallback");
        }
    }
}
