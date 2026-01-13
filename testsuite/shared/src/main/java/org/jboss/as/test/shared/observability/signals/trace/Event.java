/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import java.util.List;

import org.apache.commons.collections.KeyValue;

public class Event {
    private final long timeUnixNano;
    private final String name;
    private final List<KeyValue> attributes;
    private final int droppedAttributesCount;

    public Event(long timeUnixNano, String name, List<KeyValue> attributes, int droppedAttributesCount) {
        this.timeUnixNano = timeUnixNano;
        this.name = name;
        this.attributes = attributes;
        this.droppedAttributesCount = droppedAttributesCount;
    }

    public long getTimeUnixNano() {
        return timeUnixNano;
    }

    public String getName() {
        return name;
    }

    public List<KeyValue> getAttributes() {
        return attributes;
    }

    public int getDroppedAttributesCount() {
        return droppedAttributesCount;
    }
}
