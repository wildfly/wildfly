/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import java.util.ArrayList;
import java.util.List;

public record Trace(String traceId, List<Span> spans) {
    public Trace(String traceId) {
        this(traceId, new ArrayList<>());
    }
    public void addSpan(Span span) {
        spans.add(span);
    }
}
