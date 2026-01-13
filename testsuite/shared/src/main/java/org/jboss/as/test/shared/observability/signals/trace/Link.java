/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import java.util.List;

import org.apache.commons.collections.KeyValue;

public class Link {
    private final String traceId;
    private final String spanId;
    private final String traceState;
    private final List<KeyValue> attributes;
    private final int droppedAttributesCount;
    private final int flags;

    public Link(String traceId,
                String spanId,
                String traceState,
                List<KeyValue> attributes,
                int droppedAttributesCount,
                int flags) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceState = traceState;
        this.attributes = attributes;
        this.droppedAttributesCount = droppedAttributesCount;
        this.flags = flags;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getTraceState() {
        return traceState;
    }

    public List<KeyValue> getAttributes() {
        return attributes;
    }

    public int getDroppedAttributesCount() {
        return droppedAttributesCount;
    }

    public int getFlags() {
        return flags;
    }
}
