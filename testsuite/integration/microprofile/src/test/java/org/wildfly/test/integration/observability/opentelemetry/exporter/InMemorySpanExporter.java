/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.exporter;

import static java.util.Comparator.comparingLong;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.awaitility.Awaitility;
import org.junit.Assert;

@ApplicationScoped
public class InMemorySpanExporter implements SpanExporter {
    private boolean isStopped = false;
    private final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();
    private final Duration pollDelay = Duration.of(5, ChronoUnit.SECONDS);

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isStopped) {
            return CompletableResultCode.ofFailure();
        }
        finishedSpanItems.addAll(spans);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        finishedSpanItems.clear();
        isStopped = true;
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Careful when retrieving the list of finished spans. There is a chance when the response is already sent to the
     * client and the server still writing the end of the spans. This means that a response is available to assert from
     * the test side but not all spans may be available yet. For this reason, this method requires the number of
     * expected spans.
     */
    public List<SpanData> getFinishedSpanItems(int spanCount) {
        assertSpanCount(spanCount);
        return finishedSpanItems.stream().sorted(comparingLong(SpanData::getStartEpochNanos).reversed())
                .collect(Collectors.toList());
    }

    public void assertSpanCount(int spanCount) {
        try {
            Awaitility.await().pollDelay(pollDelay)
                    .atMost(pollDelay.multipliedBy(2))
                    .untilAsserted(() -> Assert.assertEquals(spanCount, finishedSpanItems.size()));
        } catch (RuntimeException e) {
            String spanNames = finishedSpanItems.stream().map(SpanData::getName).collect(Collectors.joining("\n"));
            throw new AssertionError("Failed to get expected spans. Got:\n" + spanNames, e);
        }
    }

    public void reset() {
        finishedSpanItems.clear();
    }
}
