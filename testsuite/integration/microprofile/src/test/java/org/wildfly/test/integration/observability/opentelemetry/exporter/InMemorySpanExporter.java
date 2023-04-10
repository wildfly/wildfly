/*
 * Copyright (c) 2016-2022 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.observability.opentelemetry.exporter;

import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;
import org.awaitility.Awaitility;
import org.junit.Assert;

@ApplicationScoped
public class InMemorySpanExporter implements SpanExporter {
    private boolean isStopped = false;
    private final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();

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
            Awaitility.await().pollDelay(3, SECONDS).atMost(30, SECONDS)
                    .untilAsserted(() -> Assert.assertEquals(spanCount, finishedSpanItems.size()));
        } catch (RuntimeException e) {
            String spanNames = finishedSpanItems.stream().map(SpanData::getName).collect(Collectors.joining("\n"));
            throw new AssertionError("Failed to get expected spans. Got:\n" + spanNames, e);
        }
    }

    public void reset() {
        finishedSpanItems.clear();
    }

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
}
