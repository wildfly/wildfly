package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import org.apache.commons.collections.map.HashedMap;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReactiveMessagingOtelAssertUtils {

    private static final String disabledTracingDestination = "disabled-tracing";
    private static final String tracedDestinationTemplate = "test%s";
    private static final String receiveSpanTemplate = "%s receive";
    private static final String publishSpanTemplate = "%s publish";
    private static final String deploymentNameTemplate = "mp-rm-%s-otel";
    private static final String postSpanTemplate = "POST /%s/";

    public interface TraceChecker {
        boolean areValidTraces(List<JaegerTrace> traces);
        String errorMessaage();
    }
    public static TraceChecker createChecker(int expectedCount, TraceExpectation expectedTrace) {
        return new TraceChecker() {
            @Override
            public boolean areValidTraces(List<JaegerTrace> traces)  {
                int count = 0;
                for (JaegerTrace trace : traces) {
                    Map<String, Integer> spanOccurance = new HashedMap();
                    for (JaegerSpan span : trace.getSpans()) {
                        spanOccurance.compute(span.getOperationName(), (s, integer) -> integer == null ? 1 : integer + 1);
                    }
                    boolean allGood = true;

                    Set<String> toCheck = new HashSet<>(spanOccurance.keySet());
                    for (SpanWithExpectedCount spanWithExpectedCount : expectedTrace.list) {
                        String expectedOpName = spanWithExpectedCount.span();
                        if ((!spanOccurance.containsKey(expectedOpName) && spanWithExpectedCount.expectedCount() == 0) ||
                                (spanOccurance.containsKey(expectedOpName) && spanOccurance.get(expectedOpName) == spanWithExpectedCount.expectedCount())) {
                            toCheck.remove(expectedOpName);
                        } else {
                            allGood = false;
                            break;
                        }
                    }
                    if (allGood && toCheck.isEmpty()) {
                        count++;
                    }
                }
                return expectedCount == count;
            }

            @Override
            public String errorMessaage() {
                StringBuilder sb = new StringBuilder("Unable to find %d times [".formatted(expectedCount));
                expectedTrace.list.forEach(sb::append);
                return sb.append("]").toString();
            }
        };
    }

    public static TraceExpectation spanSet(String connectorSufix) {
        return new TraceExpectation(connectorSufix);
    }

    static class TraceExpectation {
        private final String tracedDestination;
        private final String deploymentName;
        List<SpanWithExpectedCount> list = new ArrayList<>();
        Map<String, SpanWithExpectedCount> spanToRecord = new HashedMap();
        private TraceExpectation(String connectorSufix){
            this.deploymentName = deploymentNameTemplate.formatted(connectorSufix);
            this.tracedDestination = tracedDestinationTemplate.formatted(connectorSufix);
        };

        TraceExpectation singleDisabledReceive() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(receiveSpanTemplate.formatted(disabledTracingDestination), 1);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation singleDisabledPublish() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(publishSpanTemplate.formatted(disabledTracingDestination), 1);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation zeroDisabledReceiveAndPublish() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(receiveSpanTemplate.formatted(disabledTracingDestination), 0);
            list.add(span);
            spanToRecord.put(span.span(), span);
            span = new SpanWithExpectedCount(publishSpanTemplate.formatted(disabledTracingDestination), 0);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation singlePost() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(postSpanTemplate.formatted(deploymentName), 1);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation zeroPost() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(postSpanTemplate.formatted(deploymentName), 0);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation singleTracedReceive() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(receiveSpanTemplate.formatted(tracedDestination), 1);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }

        TraceExpectation zeroTracedReceive() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(receiveSpanTemplate.formatted(tracedDestination), 0);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation singleTracedPublish() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(publishSpanTemplate.formatted(tracedDestination), 1);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
        TraceExpectation zeroTracedPublish() {
            SpanWithExpectedCount span = new SpanWithExpectedCount(publishSpanTemplate.formatted(tracedDestination), 0);
            list.add(span);
            spanToRecord.put(span.span(), span);
            return this;
        }
    }
    record SpanWithExpectedCount(String span, int expectedCount) {}
}
